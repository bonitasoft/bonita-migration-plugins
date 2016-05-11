CREATE TABLE waiting_event (
	tenantid NUMERIC(19, 0) NOT NULL,
  	id NUMERIC(19, 0) NOT NULL,
  	kind NVARCHAR(15) NOT NULL,
  	eventType NVARCHAR(50),
  	messageName NVARCHAR(255),
  	signalName NVARCHAR(255),
  	errorCode NVARCHAR(255),
  	processName NVARCHAR(150),
  	flowNodeName NVARCHAR(50),
  	flowNodeDefinitionId NUMERIC(19, 0),
  	subProcessId NUMERIC(19, 0),
  	processDefinitionId NUMERIC(19, 0),
  	rootProcessInstanceId NUMERIC(19, 0),
  	parentProcessInstanceId NUMERIC(19, 0),
  	flowNodeInstanceId NUMERIC(19, 0),
  	relatedActivityInstanceId NUMERIC(19, 0),
  	locked BIT,
  	active BIT,
  	progress TINYINT,
  	correlation1 NVARCHAR(128),
  	correlation2 NVARCHAR(128),
  	correlation3 NVARCHAR(128),
  	correlation4 NVARCHAR(128),
  	correlation5 NVARCHAR(128),
  	PRIMARY KEY (tenantid, id)
)
@@
CREATE INDEX idx_waiting_event ON waiting_event (progress, tenantid, kind, locked, active)
@@
INSERT INTO waiting_event
    (TENANTID, ID, KIND, EVENTTYPE, MESSAGENAME, SIGNALNAME, ERRORCODE, PROCESSNAME, FLOWNODENAME, FLOWNODEDEFINITIONID, SUBPROCESSID, PROCESSDEFINITIONID, ROOTPROCESSINSTANCEID, PARENTPROCESSINSTANCEID, FLOWNODEINSTANCEID, RELATEDACTIVITYINSTANCEID, LOCKED, ACTIVE, PROGRESS, CORRELATION1, CORRELATION2, CORRELATION3, CORRELATION4, CORRELATION5)
    VALUES(1, 0, '', '', '', '', '', '', '', 0, 0, 123456789000, 0, 0, 0, 0, false, false, 0, '', '', '', '', '')
@@
INSERT INTO waiting_event
    (TENANTID, ID, KIND, EVENTTYPE, MESSAGENAME, SIGNALNAME, ERRORCODE, PROCESSNAME, FLOWNODENAME, FLOWNODEDEFINITIONID, SUBPROCESSID, PROCESSDEFINITIONID, ROOTPROCESSINSTANCEID, PARENTPROCESSINSTANCEID, FLOWNODEINSTANCEID, RELATEDACTIVITYINSTANCEID, LOCKED, ACTIVE, PROGRESS, CORRELATION1, CORRELATION2, CORRELATION3, CORRELATION4, CORRELATION5)
    VALUES(1, 1, '', '', '', '', '', '', '', 0, 0, 8888888888888, 0, 0, 0, 0, false, false, 0, '', '', '', '', '')
@@
INSERT INTO waiting_event
    (TENANTID, ID, KIND, EVENTTYPE, MESSAGENAME, SIGNALNAME, ERRORCODE, PROCESSNAME, FLOWNODENAME, FLOWNODEDEFINITIONID, SUBPROCESSID, PROCESSDEFINITIONID, ROOTPROCESSINSTANCEID, PARENTPROCESSINSTANCEID, FLOWNODEINSTANCEID, RELATEDACTIVITYINSTANCEID, LOCKED, ACTIVE, PROGRESS, CORRELATION1, CORRELATION2, CORRELATION3, CORRELATION4, CORRELATION5)
    VALUES(1, 2, '', '', '', '', '', '', '', 0, 0, 9999999999999, 0, 0, 0, 0, false, false, 0, '', '', '', '', '')
@@

CREATE TABLE process_definition (
  tenantid NUMERIC(19, 0) NOT NULL,
  id NUMERIC(19, 0) NOT NULL,
  processId NUMERIC(19, 0) NOT NULL,
  name NVARCHAR(150) NOT NULL,
  version NVARCHAR(50) NOT NULL,
  description NVARCHAR(255),
  deploymentDate NUMERIC(19, 0) NOT NULL,
  deployedBy NUMERIC(19, 0) NOT NULL,
  activationState NVARCHAR(30) NOT NULL,
  configurationState NVARCHAR(30) NOT NULL,
  displayName NVARCHAR(75),
  displayDescription NVARCHAR(255),
  lastUpdateDate NUMERIC(19, 0),
  categoryId NUMERIC(19, 0),
  iconPath NVARCHAR(255),
  content_tenantid NUMERIC(19, 0) NOT NULL,
  content_id NUMERIC(19, 0) NOT NULL,
  PRIMARY KEY (tenantid, id),
  UNIQUE (tenantid, name, version)
)
@@
INSERT INTO process_definition
    (TENANTID, ID, PROCESSID, NAME, VERSION, DESCRIPTION, DEPLOYMENTDATE, DEPLOYEDBY, ACTIVATIONSTATE, CONFIGURATIONSTATE, DISPLAYNAME, DISPLAYDESCRIPTION, LASTUPDATEDATE, CATEGORYID, ICONPATH, CONTENT_TENANTID, CONTENT_ID)
    VALUES(1, 0, 123456789000, 'process', '1.0', '', 0, 0, '', '', '', '', 0, 0, '', 0, 0)
@@