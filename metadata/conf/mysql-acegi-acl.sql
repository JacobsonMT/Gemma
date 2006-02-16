-- Note: The table names used here (including lack of capitalization) 
-- are those defined by default in org.acegisecurity.acl.basic.jdbc.JdbcExtendedDaoImpl
-- Don't change them!

DROP TABLE IF EXISTS acl_object_identity;
CREATE TABLE acl_object_identity (
  id int(11) NOT NULL auto_increment,
  object_identity varchar(255) NOT NULL,
  parent_object int(11) default NULL,
  acl_class varchar(255) NOT NULL,
  PRIMARY KEY  (id),
  UNIQUE KEY object_identity (object_identity)
) TYPE=InnoDB;

DROP TABLE IF EXISTS acl_permission;
CREATE TABLE acl_permission (
  id int(11) NOT NULL auto_increment,
  acl_object_identity int(11) NOT NULL,
  recipient varchar(100) NOT NULL,
  mask int(11) NOT NULL,
  PRIMARY KEY  (id)
) TYPE=InnoDB;

--- (id, object identity (of form class:getId()), parent object, acl class)
INSERT INTO acl_object_identity VALUES (1, 'globalDummyParent:1', null, 'org.acegisecurity.acl.basic.SimpleAclEntry');

--- (id, acl object identity, recepient (principal username), mask)
INSERT INTO acl_permission VALUES (null, 1, 'administrator', 1);


--- Mask integer 0  = no permissions
--- Mask integer 1  = administrator
--- Mask integer 2  = read
--- Mask integer 6  = read and write permissions
--- Mask integer 14 = read and write and create permissions
