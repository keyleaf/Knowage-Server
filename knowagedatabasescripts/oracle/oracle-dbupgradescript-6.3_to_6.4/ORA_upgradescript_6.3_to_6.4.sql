ALTER TABLE SBI_CROSS_NAVIGATION ADD DESCRIPTION VARCHAR2(200) DEFAULT NULL;
ALTER TABLE SBI_CROSS_NAVIGATION ADD BREADCRUMB VARCHAR2(200) DEFAULT NULL;

CREATE TABLE SBI_NEWS (
  ID INTEGER NOT NULL,
  NAME VARCHAR2(200) DEFAULT NULL,
  DESCRIPTION VARCHAR2(400) DEFAULT NULL,
  ACTIVE SMALLINT DEFAULT 1,
  NEWS VARCHAR2(4000) DEFAULT NULL,
  MANUAL SMALLINT DEFAULT 1,
  EXPIRATION_DATE timestamp DEFAULT NULL,
  USER_IN VARCHAR2(100) NOT NULL,
  USER_UP VARCHAR2(100) DEFAULT NULL,
  USER_DE VARCHAR2(100) DEFAULT NULL,
  TIME_IN timestamp NOT NULL,
  TIME_UP timestamp DEFAULT NULL,
  TIME_DE timestamp DEFAULT NULL,
  SBI_VERSION_IN VARCHAR2(10) DEFAULT NULL,
  SBI_VERSION_UP VARCHAR2(10) DEFAULT NULL,
  SBI_VERSION_DE VARCHAR2(10) DEFAULT NULL,
  META_VERSION VARCHAR2(100) DEFAULT NULL,
  ORGANIZATION VARCHAR2(20) DEFAULT NULL,
  CATEGORY_ID INTEGER DEFAULT NULL,
  PRIMARY KEY (ID)
);
ALTER TABLE SBI_NEWS ADD CONSTRAINT VALUE_ID FOREIGN KEY (CATEGORY_ID) REFERENCES SBI_DOMAINS (VALUE_ID);

CREATE TABLE SBI_NEWS_READ (
  ID INTEGER NOT NULL,
  USER_ID VARCHAR2(100) NOT NULL,
  NEWS_ID INTEGER NOT NULL,
  USER_IN VARCHAR2(100) NOT NULL,
  USER_UP VARCHAR2(100) DEFAULT NULL,
  USER_DE VARCHAR2(100) DEFAULT NULL,
  TIME_IN timestamp NOT NULL,
  TIME_UP timestamp DEFAULT NULL,
  TIME_DE timestamp DEFAULT NULL,
  SBI_VERSION_IN VARCHAR2(10) DEFAULT NULL,
  SBI_VERSION_UP VARCHAR2(10) DEFAULT NULL,
  SBI_VERSION_DE VARCHAR2(10) DEFAULT NULL,
  META_VERSION VARCHAR2(100) DEFAULT NULL,
  ORGANIZATION VARCHAR2(20) DEFAULT NULL,
  PRIMARY KEY (ID)
);
ALTER TABLE SBI_NEWS_READ ADD CONSTRAINT FK_NEWS_ID FOREIGN KEY (NEWS_ID) REFERENCES SBI_NEWS (ID) ON DELETE CASCADE;


CREATE TABLE SBI_NEWS_ROLES (
  NEWS_ID INTEGER NOT NULL,
  EXT_ROLE_ID INTEGER NOT NULL,
  PRIMARY KEY (NEWS_ID,EXT_ROLE_ID)
);
ALTER TABLE SBI_NEWS_ROLES ADD CONSTRAINT NEWS_ID FOREIGN KEY (NEWS_ID) REFERENCES SBI_NEWS (ID) ON DELETE CASCADE;
ALTER TABLE SBI_NEWS_ROLES ADD CONSTRAINT EXT_ROLE_ID FOREIGN KEY (EXT_ROLE_ID) REFERENCES SBI_EXT_ROLES (EXT_ROLE_ID) ON DELETE CASCADE;