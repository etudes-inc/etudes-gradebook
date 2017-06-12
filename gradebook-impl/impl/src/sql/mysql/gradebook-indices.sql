---------------------------------------------------------------------------------- 
-- $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/sql/mysql/gradebook-indices.sql $ 
-- $Id: gradebook-indices.sql 10953 2015-05-22 21:18:37Z murthyt $ 
----------------------------------------------------------------------------------- 
-- 
-- Copyright (c) 2015 Etudes, Inc. 
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

-- missing indices

-- gradebook_context_categories
ALTER TABLE gradebook_context_categories ADD INDEX GCC_GRADEBOOK_ID_CATEGORY_TYPE (GRADEBOOK_ID ASC, CATEGORY_TYPE ASC);

-- 