MERGE branch AS t
USING (VALUES
    ('CSE',   'Computer Science and Engineering'),
    ('AIML',  'Artificial Intelligence and Machine Learning'),
    ('IT',    'Information Technology'),
    ('ECE',   'Electronics and Communication Engineering'),
    ('EEE',   'Electrical and Electronics Engineering'),
    ('MECH',  'Mechanical Engineering'),
    ('CIVIL', 'Civil Engineering')
) AS s(code, name)
ON t.code = s.code
WHEN NOT MATCHED THEN
    INSERT (code, name) VALUES (s.code, s.name);
