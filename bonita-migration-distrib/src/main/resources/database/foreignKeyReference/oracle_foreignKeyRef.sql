SELECT
    c.TABLE_NAME,
    c.CONSTRAINT_NAME,
    c2.TABLE_NAME as r_table_name
FROM
    user_CONSTRAINTS C,
    user_CONSTRAINTS C2
WHERE
    LOWER( c2.TABLE_NAME ) = LOWER(?)
    AND c.CONSTRAINT_TYPE = 'R'
    AND c.R_CONSTRAINT_NAME=c2.CONSTRAINT_NAME