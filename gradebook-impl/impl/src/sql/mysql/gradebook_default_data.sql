---------------------------------------------------------------------------------- 
-- $URL: https://source.etudes.org/svn/apps/gradebook/trunk/gradebook-impl/impl/src/sql/mysql/gradebook_default_data.sql $ 
-- $Id: gradebook_default_data.sql 9561 2014-12-17 17:49:38Z murthyt $ 
----------------------------------------------------------------------------------- 
-- 
-- Copyright (c) 2014 Etudes, Inc. 
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

---- insert default data

-- gradebook_grading_scale dafulat data
INSERT INTO gradebook_grading_scale(NAME, CODE, TYPE, VERSION, LOCKED)
VALUES ( 'Letter Grades', 'LetterGrade', 1, 1, 0);

INSERT INTO gradebook_grading_scale(NAME, CODE, TYPE, VERSION, LOCKED)
VALUES ( 'Letter Grades with +/-', 'LetterGradePlusMinus', 2, 1, 0);

INSERT INTO gradebook_grading_scale(NAME, CODE, TYPE, VERSION, LOCKED)
VALUES ( 'Pass / Not Pass', 'PassNotPass', 3, 1, 0);

---------------------------------------------------------------------------------------
---- table for grading scale percent's default data is gradebook_grading_scale_grades 
---------------------------------------------------------------------------------------
-- Letter Grades
INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 1), '90', 'A', 1);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 1), '80', 'B', 2);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 1), '70', 'C', 3);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 1), '60', 'D', 4);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 1), '0', 'F', 5);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 1), '0', 'I', 6);

-- LetterGradePlusMinus
INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 2), '100', 'A+', 1);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 2), '95', 'A', 2);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 2), '90', 'A-', 3);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 2), '87', 'B+', 4);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 2), '83', 'B', 5);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 2), '80', 'B-', 6);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 2), '77', 'C+', 7);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 2), '73', 'C', 8);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 2), '70', 'C-', 9);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 2), '67', 'D+', 10);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 2), '63', 'D', 11);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 2), '60', 'D-', 12);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 2), '0', 'F', 13);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 2), '0', 'I', 14);

-- PassNotPass
INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 3), '75', 'P', 1);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 3), '0', 'NP', 2);

INSERT INTO gradebook_grading_scale_grades (GRADING_SCALE_ID, PERCENT, LETTER_GRADE, SEQUENCE)
VALUES((SELECT ID FROM gradebook_grading_scale WHERE TYPE = 3), '0', 'I', 3);