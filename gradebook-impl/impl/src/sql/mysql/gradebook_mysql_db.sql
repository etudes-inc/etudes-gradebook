---------------------------------------------------------------------------------- 
-- $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/sql/mysql/gradebook_mysql_db.sql $ 
-- $Id: gradebook_mysql_db.sql 12452 2016-01-06 00:29:51Z murthyt $ 
----------------------------------------------------------------------------------- 
-- 
-- Copyright (c) 2014, 2015, 2016 Etudes, Inc. 
-- 
-- Licensed under the Apache License, Version 2.0 (the "License"); 
-- you may not use this file except in compliance with the License. 
-- You may obtain a copy of the License at 
-- 
-- http://www.apache.org/licenses/LICENSE-2.0 
-- 
-- Unless required by applicable law or agreed to in writing, software 
-- distributed under the License is distributed on an "AS IS" BASIS, 
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
-- See the License for the specific language governing permissions and 
-- limitations under the License.
----------------------------------------------------------------------------------

-- for grading options - table - gradebook_grading_scale - with default values
CREATE TABLE gradebook_grading_scale (
  ID BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  NAME VARCHAR(255) NOT NULL,
  CODE VARCHAR(99) NOT NULL,
  TYPE SMALLINT UNSIGNED NOT NULL,
  VERSION INT UNSIGNED NOT NULL DEFAULT 1,
  LOCKED TINYINT(1) UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE INDEX type_UNIQUE (type),
  UNIQUE INDEX code_UNIQUE (code));

---------------

-- gradebook_grading_scale_grades - default letter grades and percent's
CREATE TABLE gradebook_grading_scale_grades (
  ID BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  GRADING_SCALE_ID BIGINT UNSIGNED NOT NULL,
  PERCENT FLOAT NOT NULL,
  LETTER_GRADE VARCHAR(99) NOT NULL,
  SEQUENCE TINYINT UNSIGNED NOT NULL,
  PRIMARY KEY (ID),
  UNIQUE INDEX GRADING_SCALE_ID_LETTER_GRADE (GRADING_SCALE_ID, LETTER_GRADE),
  UNIQUE INDEX GRADING_SCALE_ID_SEQUENCE (GRADING_SCALE_ID, SEQUENCE));
  
  

  -- GRADING_SCALE_ID references ID of gradebook_grading_scale table

---------------
  
-- for site gradebook - table - gradebook_gradebook
CREATE TABLE gradebook_gradebook (
  ID BIGINT UNSIGNED NULL AUTO_INCREMENT,
  CONTEXT VARCHAR(99) NOT NULL,
  SELECTED_GRADING_SCALE_ID BIGINT UNSIGNED NOT NULL,
  SHOW_LETTER_GRADE TINYINT(1) UNSIGNED NOT NULL DEFAULT 0,
  RELEASE_GRADES_TYPE TINYINT(1) UNSIGNED NOT NULL DEFAULT 2,
  CATEGORY_TYPE SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  DROP_LOWEST_SCORE TINYINT(1) UNSIGNED NOT NULL DEFAULT 0,
  BOOST_USER_GRADES_TYPE SMALLINT UNSIGNED NULL DEFAULT NULL,
  BOOST_USER_GRADES_BY FLOAT UNSIGNED NULL DEFAULT NULL,
  VERSION INT UNSIGNED NOT NULL DEFAULT 1,
  CREATED_BY_USER VARCHAR(99) NOT NULL,
  CREATED_DATE DATETIME NOT NULL,
  MODIFIED_BY_USER VARCHAR(99) NULL,
  MODIFIED_DATE DATETIME NULL,  
  PRIMARY KEY (ID),
  UNIQUE INDEX context_UNIQUE (CONTEXT));

 -- GRADING_SCALE_ID references to other gradebook table (table gradebook_grading_scale - column 'id')
 -- CONTEXT is site_id

---------------

-- gradebook_context_grading_scale_grades - actual letter grades for sites
CREATE TABLE gradebook_context_grading_scale_grades (
  ID BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  GRADEBOOK_ID BIGINT UNSIGNED NOT NULL,
  GRADING_SCALE_ID BIGINT UNSIGNED NOT NULL,
  PERCENT FLOAT NOT NULL,
  LETTER_GRADE VARCHAR(99) NOT NULL,
  SEQUENCE TINYINT UNSIGNED NOT NULL,
  PRIMARY KEY (ID),
  UNIQUE INDEX GRADEBOOK_ID_GRADING_SCALE_ID_LETTER_GRADE (GRADEBOOK_ID, GRADING_SCALE_ID, LETTER_GRADE),
  UNIQUE INDEX GRADEBOOK_ID_GRADING_SCALE_ID_SEQUENCE (GRADEBOOK_ID, GRADING_SCALE_ID, SEQUENCE));
  
  -- GRADEBOOK_ID references  ID of gradebook_gradebook table
  -- GRADING_SCALE_ID references ID of gradebook_grading_scale table
  
 ---------------
 
  --gradebook_user_grades - assigned or entered user grades in the site
  CREATE TABLE gradebook_user_grades (
  ID BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  GRADEBOOK_ID BIGINT UNSIGNED NOT NULL,
  STUDENT_ID VARCHAR(99) NOT NULL,
  ASSIGNED_LETTER_GRADE VARCHAR(99) NULL,
  ASSIGNED_BY_USER VARCHAR(99) NOT NULL,
  ASSIGNED_DATE DATETIME NOT NULL,
  PRIMARY KEY (ID),
  UNIQUE KEY GRADEBOOK_ID_USER_ID (GRADEBOOK_ID, STUDENT_ID));
  
  -- GRADEBOOK_ID references  ID of gradebook_gradebook table
 ---------------
  --gradebook_user_grades_history - history of users overrriden(assigned or entered) grade
  CREATE TABLE gradebook_user_grades_history (
  ID bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  GRADEBOOK_ID BIGINT UNSIGNED NOT NULL,
  STUDENT_ID VARCHAR(99) NOT NULL,
  ASSIGNED_LETTER_GRADE VARCHAR(99) NULL,
  ASSIGNED_BY_USER VARCHAR(99) NOT NULL,
  ASSIGNED_DATE DATETIME NOT NULL,
  PRIMARY KEY (ID),
  UNIQUE KEY GRADEBOOK_ID_USER_ID (GRADEBOOK_ID,STUDENT_ID));
  -- GRADEBOOK_ID references  ID of gradebook_gradebook table
  
 ---------------
 --gradebook_context_categories for context categories(standard or custom)
  CREATE TABLE gradebook_context_categories (
  ID BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  GRADEBOOK_ID BIGINT UNSIGNED NOT NULL,
  TITLE VARCHAR(255) NOT NULL,
  WEIGHT FLOAT DEFAULT NULL,
  WEIGHT_DISTRIBUTION SMALLINT DEFAULT NULL,
  DROP_NUMBER_OF_LOWEST_SCORES SMALLINT DEFAULT NULL,
  IS_EXTRA_CREDIT TINYINT(1) NOT NULL DEFAULT '0',
  CATEGORY_ORDER BIGINT UNSIGNED NOT NULL,
  CATEGORY_TYPE SMALLINT UNSIGNED NOT NULL,
  STANDARD_CATEGORY_CODE SMALLINT UNSIGNED NULL,
  CREATED_BY_USER VARCHAR(99) NOT NULL,
  CREATED_DATE DATETIME NOT NULL,
  MODIFIED_BY_USER VARCHAR(99) NULL,
  MODIFIED_DATE DATETIME NULL,
  PRIMARY KEY (ID),
  INDEX GCC_GRADEBOOK_ID_CATEGORY_TYPE (GRADEBOOK_ID,CATEGORY_TYPE));
  -- GRADEBOOK_ID references  ID of gradebook_gradebook table
  
  --------------
  --gradebook_context_type_item_map for context category and item map
  CREATE TABLE gradebook_context_category_item_map (
  ID BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  CATEGORY_ID BIGINT UNSIGNED NOT NULL,
  ITEM_ID VARCHAR(255) NOT NULL,
  ITEM_ORDER INT UNSIGNED NOT NULL,
  PRIMARY KEY (ID),
  UNIQUE INDEX GRADEBOOK_CATEGORY_ID_ITEM_ID (CATEGORY_ID, ITEM_ID));
  -- CATEGORY_ID references  ID of gradebook_context_categories table
  
  -------------
  -- gradebook_instructor_student_notes for instructor notes for students
  CREATE TABLE gradebook_instructor_student_notes (
  ID BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  GRADEBOOK_ID BIGINT UNSIGNED NOT NULL,
  STUDENT_ID VARCHAR(99) NOT NULL,
  NOTES LONGTEXT NULL,
  ADDED_BY_USER VARCHAR(99) NOT NULL,
  MODIFED_BY_USER VARCHAR(99) NULL,
  ADDED_DATE DATETIME NOT NULL,
  MODIFIED_DATE DATETIME NULL,
  PRIMARY KEY (ID),
  UNIQUE INDEX GISN_GRADEBOOK_ID_STUDENT_ID (GRADEBOOK_ID ASC, STUDENT_ID ASC));