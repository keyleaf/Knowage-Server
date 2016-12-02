/*
 * Knowage, Open Source Business Intelligence suite
 * Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.

 * Knowage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Knowage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.eng.spagobi.tools.dataset;

import it.eng.spago.error.EMFUserError;
import it.eng.spagobi.commons.bo.UserProfile;
import it.eng.spagobi.commons.dao.DAOFactory;
import it.eng.spagobi.tools.dataset.bo.AbstractJDBCDataset;
import it.eng.spagobi.tools.dataset.bo.IDataSet;
import it.eng.spagobi.tools.dataset.cache.ICache;
import it.eng.spagobi.tools.dataset.cache.SpagoBICacheConfiguration;
import it.eng.spagobi.tools.dataset.cache.SpagoBICacheManager;
import it.eng.spagobi.tools.dataset.common.behaviour.QuerableBehaviour;
import it.eng.spagobi.tools.dataset.common.datastore.DataStore;
import it.eng.spagobi.tools.dataset.common.datastore.IDataStore;
import it.eng.spagobi.tools.dataset.dao.IDataSetDAO;
import it.eng.spagobi.tools.dataset.graph.EdgeGroup;
import it.eng.spagobi.tools.dataset.graph.LabeledEdge;
import it.eng.spagobi.tools.datasource.bo.IDataSource;
import it.eng.spagobi.utilities.cache.CacheItem;
import it.eng.spagobi.utilities.exceptions.SpagoBIException;
import it.eng.spagobi.utilities.exceptions.SpagoBIRuntimeException;
import it.eng.spagobi.utilities.sql.SqlUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.metamodel.data.DataSet;
import org.apache.metamodel.data.Row;
import org.jgrapht.graph.Pseudograph;

/**
 * @author Alessandro Portosa (alessandro.portosa@eng.it)
 *
 */

public class AssociativeLogicManager {

	private final static int IN_CLAUSE_LIMIT = 999;

	private final IDataSource cacheDataSource;
	private final ICache cache;
	private Map<EdgeGroup, Set<String>> edgeGroupValues;
	private final Map<String, Map<String, String>> datasetToAssociations;
	private Map<String, Set<EdgeGroup>> datasetToEdgeGroup;
	private Map<EdgeGroup, Set<String>> edgeGroupToDataset;
	private final Pseudograph<String, LabeledEdge<String>> graph;
	private final Map<String, String> datasetToTableName;
	private final Map<String, IDataSource> datasetToDataSource;
	private final Map<String, IDataStore> datasetToDataStore;
	private final Map<String, String> selections;
	private final Set<String> realtimeDatasets;
	private final Map<String, IDataSet> labelToDataset;
	private final Map<String, Map<String, String>> datasetParameters;
	private final Set<String> documents;

	private UserProfile userProfile;

	static private Logger logger = Logger.getLogger(AssociativeLogicManager.class);

	public AssociativeLogicManager(Pseudograph<String, LabeledEdge<String>> graph, Map<String, Map<String, String>> datasetToAssociations,
			Map<String, String> selections, Set<String> realtimeDatasets, Map<String, Map<String, String>> datasetParameters, Set<String> documents) {
		this.graph = graph;
		this.datasetToAssociations = datasetToAssociations;
		this.selections = selections;
		this.realtimeDatasets = realtimeDatasets;
		this.datasetParameters = datasetParameters;
		this.documents = documents;

		this.datasetToTableName = new HashMap<String, String>();
		this.datasetToDataSource = new HashMap<String, IDataSource>();
		this.datasetToDataStore = new HashMap<String, IDataStore>();
		this.labelToDataset = new HashMap<String, IDataSet>();

		this.cacheDataSource = SpagoBICacheConfiguration.getInstance().getCacheDataSource();
		this.cache = SpagoBICacheManager.getCache();

		this.userProfile = null;
	}

	public Map<EdgeGroup, Set<String>> process() throws Exception {
		if (cacheDataSource == null) {
			throw new SpagoBIException("Unable to get cache datasource, the value of [dataSource] is [null]");
		}
		if (cache == null) {
			throw new SpagoBIException("Unable to get cache, the value of [cache] is [null]");
		}

		// (0) generate the dataset mappings
		initDatasetMappings();

		// (1) generate the starting set of values for each associations
		initProcess();

		// (2) user click on widget -> selection!
		for (String datasetSelected : selections.keySet()) {
			if (!documents.contains(datasetSelected)) {
				String filterSelected = selections.get(datasetSelected);
				calculateDatasets(datasetSelected, null, filterSelected);
			}
		}

		return edgeGroupValues;
	}

	private void initDatasetMappings() throws EMFUserError, SpagoBIException {
		IDataSetDAO dataSetDao = DAOFactory.getDataSetDAO();
		if (getUserProfile() != null) {
			dataSetDao.setUserProfile(userProfile);
		}

		for (String v1 : graph.vertexSet()) {
			if (!documents.contains(v1)) {
				// the vertex is the dataset label
				IDataSet dataSet = dataSetDao.loadDataSetByLabel(v1);
				if (dataSet != null) {
					Map<String, String> parametersValues = datasetParameters.get(v1);
					dataSet.setParamsMap(parametersValues);

					labelToDataset.put(v1, dataSet);

					if (dataSet.isPersisted() && !dataSet.isPersistedHDFS()) {
						datasetToTableName.put(v1, dataSet.getPersistTableName());
						datasetToDataSource.put(v1, dataSet.getDataSourceForWriting());
						realtimeDatasets.remove(v1);
					} else if (dataSet.isFlatDataset()) {
						datasetToTableName.put(v1, dataSet.getFlatTableName());
						datasetToDataSource.put(v1, dataSet.getDataSource());
						realtimeDatasets.remove(v1);
					} else if (realtimeDatasets.contains(v1) && DatasetManagementAPI.isJDBCDataSet(dataSet)
							&& !SqlUtils.isBigDataDialect(dataSet.getDataSource().getHibDialectName())) {
						QuerableBehaviour querableBehaviour = (QuerableBehaviour) dataSet.getBehaviour(QuerableBehaviour.class.getName());
						String tableName = "(" + querableBehaviour.getStatement() + ") T";
						datasetToTableName.put(v1, tableName);
						datasetToDataSource.put(v1, dataSet.getDataSource());
						realtimeDatasets.remove(v1);
					} else if (realtimeDatasets.contains(v1)) {
						dataSet.loadData();
						datasetToDataStore.put(v1, dataSet.getDataStore());
					} else {
						String signature = dataSet.getSignature();
						CacheItem cacheItem = cache.getMetadata().getCacheItem(signature);
						if (cacheItem != null) {
							String tableName = cacheItem.getTable();
							datasetToTableName.put(v1, tableName);
							datasetToDataSource.put(v1, cacheDataSource);
						} else {
							throw new SpagoBIException("Unable to find dataset [" + v1 + "] in cache");
						}
					}
				}
			}
		}
	}

	private void initProcess() {
		edgeGroupValues = new HashMap<EdgeGroup, Set<String>>();
		datasetToEdgeGroup = new HashMap<String, Set<EdgeGroup>>();
		edgeGroupToDataset = new HashMap<EdgeGroup, Set<String>>();

		try {
			for (String v1 : graph.vertexSet()) {
				datasetToEdgeGroup.put(v1, new HashSet<EdgeGroup>());
				for (String v2 : graph.vertexSet()) {
					if (!v1.equals(v2)) {
						Set<LabeledEdge<String>> edges = graph.getAllEdges(v1, v2);
						if (!edges.isEmpty()) {
							EdgeGroup group = new EdgeGroup(edges);
							datasetToEdgeGroup.get(v1).add(group);

							if (!documents.contains(v1)) {
								String tableName = getTableName(v1);
								// PreparedStatement stmt = getPreparedQuery(dataSource.getConnection(), columnNames, cacheItem.getTable());
								String columnNames = getColumnNames(group.getOrderedEdgeNames(), v1);
								if (!columnNames.isEmpty()) {
									String query = "SELECT DISTINCT " + columnNames + " FROM " + tableName;
									Set<String> tuple = getTupleOfValues(v1, query);

									if (!edgeGroupValues.containsKey(group)) {
										edgeGroupValues.put(group, tuple);
									} else {
										edgeGroupValues.get(group).retainAll(tuple);
									}
								}
							}

							if (!edgeGroupToDataset.containsKey(group)) {
								edgeGroupToDataset.put(group, new HashSet<String>());
								edgeGroupToDataset.get(group).add(v1);
							} else {
								edgeGroupToDataset.get(group).add(v1);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			throw new SpagoBIRuntimeException("Error during the initializing of the AssociativeLogicManager", e);
		}
	}

	private Set<String> getTupleOfValues(String dataSet, String query) throws ClassNotFoundException, NamingException, SQLException {
		Set<String> tuple = new HashSet<String>(0);
		if (realtimeDatasets.contains(dataSet)) {
			IDataStore dataStore = datasetToDataStore.get(dataSet);
			if (dataStore != null) {
				logger.debug("Executing query with MetaModel: " + query);
				org.apache.metamodel.data.DataSet rs = dataStore.getMetaModelResultSet(query);
				tuple = getTupleOfValues(rs);
			} else {
				throw new SpagoBIRuntimeException("Error while retrieving the DataStore for real-time dataset with label [" + dataSet
						+ "]. It is impossible to get values from it.");
			}
		} else {
			Connection connection = null;
			Statement stmt = null;
			ResultSet rs = null;
			try {
				logger.debug("Executing query: " + query);
				connection = getDataSource(dataSet).getConnection();
				stmt = connection.createStatement();
				rs = stmt.executeQuery(query);
				tuple = getTupleOfValues(rs);
			} finally {
				if (rs != null) {
					try {
						rs.close();
					} catch (SQLException e) {
						logger.debug(e);
					}
				}
				if (stmt != null) {
					try {
						stmt.close();
					} catch (SQLException e) {
						logger.debug(e);
					}
				}
				if (connection != null) {
					try {
						connection.close();
					} catch (SQLException e) {
						logger.debug(e);
					}
				}
			}
		}
		return tuple;
	}

	private String getTableName(String datasetLabel) {
		String tableName;
		if (realtimeDatasets.contains(datasetLabel)) {
			tableName = DataStore.DEFAULT_SCHEMA_NAME + "." + DataStore.DEFAULT_TABLE_NAME;
		} else {
			tableName = datasetToTableName.get(datasetLabel);
		}
		return tableName;
	}

	private IDataSource getDataSource(String datasetLabel) {
		if (realtimeDatasets.contains(datasetLabel)) {
			return null; // with null, AbstractJDBCDataset.encapsulateColumnName returns an empty string
		} else {
			return datasetToDataSource.get(datasetLabel);
		}
	}

	private String getColumnNames(String associationNamesString, String datasetName) {
		String[] associationNames = associationNamesString.split(",");
		List<String> columnNames = new ArrayList<String>();
		IDataSource dataSource = getDataSource(datasetName);
		for (String associationName : associationNames) {
			Map<String, String> associationToColumns = datasetToAssociations.get(datasetName);
			if (associationToColumns != null) {
				String columnName = associationToColumns.get(associationName);
				if (columnName != null) {
					if (realtimeDatasets.contains(datasetName)) {
						columnName = DataStore.DEFAULT_TABLE_NAME + "." + AbstractJDBCDataset.encapsulateColumnName(columnName, null);
					} else {
						columnName = AbstractJDBCDataset.encapsulateColumnName(columnName, dataSource);
					}
					columnNames.add(columnName);
				}
			}
		}
		return StringUtils.join(columnNames.iterator(), ",");
	}

	@SuppressWarnings("unchecked")
	private void calculateDatasets(String dataset, EdgeGroup fromEdgeGroup, String filter) throws Exception {
		Set<EdgeGroup> groups = datasetToEdgeGroup.get(dataset);
		String tableName = getTableName(dataset);

		// iterate over all the associations
		for (EdgeGroup group : groups) {
			String columnNames = getColumnNames(group.getOrderedEdgeNames(), dataset);
			if (columnNames.length() > 0) {
				String query = "SELECT DISTINCT " + columnNames + " FROM " + tableName + " WHERE " + filter;

				Set<String> distinctValues = getTupleOfValues(dataset, query);

				Set<String> baseSet = edgeGroupValues.get(group);
				Set<String> intersection = new HashSet<String>(CollectionUtils.intersection(baseSet, distinctValues));
				if (!intersection.equals(baseSet)) {
					if (intersection.size() > 0) {
						edgeGroupValues.put(group, intersection);

						for (String datasetInvolved : edgeGroupToDataset.get(group)) {
							if (!documents.contains(datasetInvolved) && !datasetInvolved.equals(dataset)) {
								columnNames = getColumnNames(group.getOrderedEdgeNames(), datasetInvolved);
								if (columnNames.length() > 0) {
									String whereClauses = null;
									if (realtimeDatasets.contains(datasetInvolved)) {
										StringBuilder sb = new StringBuilder();
										String[] columnsArray = columnNames.split(",");
										for (String values : intersection) {
											String[] valuesArray = values.substring(1, values.length() - 1).split(",");
											if (sb.length() > 0) {
												sb.append(" OR ");
											}
											if (valuesArray.length > 1) {
												sb.append("(");
											}
											for (int j = 0; j < valuesArray.length; j++) {
												if (j > 0) {
													sb.append(" AND ");
												}
												sb.append(AbstractJDBCDataset.encapsulateColumnName(columnsArray[j], null));
												sb.append("=");
												sb.append(valuesArray[j]);
											}
											if (valuesArray.length > 1) {
												sb.append(")");
											}
										}
										whereClauses = sb.toString();
									} else {
										String inClauseColumns;
										String inClauseValues;
										if (intersection.size() > IN_CLAUSE_LIMIT) {
											inClauseColumns = "1," + columnNames;
											inClauseValues = getUnlimitedInClauseValues(intersection);
										} else {
											inClauseColumns = columnNames;
											inClauseValues = StringUtils.join(intersection.iterator(), ",");
										}
										whereClauses = "(" + inClauseColumns + ") IN (" + inClauseValues + ")";
									}
									// it will skip the current dataset, from which the filter is fired
									calculateDatasets(datasetInvolved, group, whereClauses);
								}
							}
						}
					} else {
						Set<String> emptySet = new HashSet<String>();
						for (EdgeGroup edgeGroup : edgeGroupValues.keySet()) {
							edgeGroupValues.put(edgeGroup, emptySet);
						}
						return;
					}
				}
			}
		}
	}

	private String getUnlimitedInClauseValues(Set<String> values) {
		Set<String> newValues = new HashSet<String>();
		for (String value : values) {
			newValues.add(value.replaceFirst("\\(", "(1,"));
		}
		return StringUtils.join(newValues.iterator(), ",");
	}

	private Set<String> getTupleOfValues(ResultSet rs) throws SQLException {
		String tuple;
		String stringDelimiter = "'";
		Set<String> tuples = new HashSet<String>();
		while (rs.next()) {
			tuple = "(";
			for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
				if (i != 1) {
					tuple += ",";
				}
				Object item = rs.getObject(i);
				tuple += stringDelimiter + (item == null ? null : item.toString()) + stringDelimiter;
			}
			tuple += ")";
			tuples.add(tuple);
		}
		return tuples;
	}

	private Set<String> getTupleOfValues(DataSet rs) {
		String tuple;
		String stringDelimiter = "'";
		Set<String> tuples = new HashSet<String>();
		while (rs.next()) {
			tuple = "(";
			for (int i = 0; i < rs.getSelectItems().length; i++) {
				Row row = rs.getRow();
				if (i > 0) {
					tuple += ",";
				}
				Object item = row.getValue(i);
				tuple += stringDelimiter + (item == null ? null : item.toString()) + stringDelimiter;
			}
			tuple += ")";
			tuples.add(tuple);
		}
		return tuples;
	}

	// @SuppressWarnings("unused")
	// private PreparedStatement getPreparedQuery(Connection connection, String[] columnNames, String tableName) throws SQLException {
	// StringBuilder sb = new StringBuilder();
	// sb.append("SELECT DISTINCT");
	// sb.append(" ");
	// for (int i = 0; i < columnNames.length; i++) {
	// if (i != 0) {
	// sb.append(",");
	// }
	// sb.append("?");
	// }
	// sb.append("FROM");
	// sb.append(" ");
	// sb.append(tableName);
	// return connection.prepareStatement(sb.toString());
	// }

	public UserProfile getUserProfile() {
		return userProfile;
	}

	public void setUserProfile(UserProfile userProfile) {
		this.userProfile = userProfile;
	}
}
