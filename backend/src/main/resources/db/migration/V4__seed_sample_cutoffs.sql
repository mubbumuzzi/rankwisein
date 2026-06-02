-- Sample data so /api/predict works before any PDF import.

INSERT INTO college_branch (college_id, branch_id, intake)
SELECT c.id, b.id, 60
FROM college c
CROSS JOIN branch b
WHERE b.code IN ('CSE', 'ECE', 'EEE', 'MECH', 'IT')
  AND NOT EXISTS (
      SELECT 1 FROM college_branch cb
      WHERE cb.college_id = c.id AND cb.branch_id = b.id
  );

INSERT INTO cutoff ([year], phase, college_id, branch_id, category, gender, closing_rank)
SELECT 2024,
       'FINAL_PHASE',
       c.id,
       br.id,
       cat.category,
       g.gender,
       (cfg.base_rank + b.boff + cat.coff + g.goff)
FROM (VALUES
        ('CBIT',     1500),
        ('VNRVJIET', 2200),
        ('GRIET',    4500),
        ('CVR',      5200),
        ('MGIT',     6800),
        ('SNIST',    8000),
        ('BVRIT',    9500),
        ('MREC',    14000),
        ('CMR',     16000),
        ('MLRIT',   18000),
        ('VJIT',    21000)
     ) AS cfg(code, base_rank)
JOIN college c ON c.code = cfg.code
CROSS JOIN (VALUES
        ('CSE',     0),
        ('IT',   1200),
        ('ECE',  3000),
        ('EEE',  6000),
        ('MECH', 9000)
     ) AS b(code, boff)
JOIN branch br ON br.code = b.code
CROSS JOIN (VALUES
        ('OC',      0),
        ('BC-B', 2500),
        ('SC-I', 9000),
        ('ST',  12000)
     ) AS cat(category, coff)
CROSS JOIN (VALUES
        ('BOYS',    0),
        ('GIRLS', 800)
     ) AS g(gender, goff)
WHERE NOT EXISTS (
    SELECT 1 FROM cutoff co
    WHERE co.[year] = 2024
      AND co.phase = 'FINAL_PHASE'
      AND co.college_id = c.id
      AND co.branch_id = br.id
      AND co.category = cat.category
      AND co.gender = g.gender
);
