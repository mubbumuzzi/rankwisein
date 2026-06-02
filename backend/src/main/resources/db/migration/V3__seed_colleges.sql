MERGE college AS t
USING (VALUES
    ('CBIT',  'Chaitanya Bharathi Institute of Technology', 'Gandipet, Hyderabad',  'Hyderabad',      1, 'https://www.cbit.ac.in'),
    ('VNRVJIET', 'VNR Vignana Jyothi Institute of Engineering and Technology', 'Bachupally, Hyderabad', 'Medchal-Malkajgiri', 1, 'https://www.vnrvjiet.ac.in'),
    ('GRIET', 'Gokaraju Rangaraju Institute of Engineering and Technology', 'Bachupally, Hyderabad', 'Medchal-Malkajgiri', 1, 'https://www.griet.ac.in'),
    ('CVR',   'CVR College of Engineering', 'Mangalpalli, Ibrahimpatnam', 'Ranga Reddy', 1, 'https://cvr.ac.in'),
    ('MGIT',  'Mahatma Gandhi Institute of Technology', 'Gandipet, Hyderabad', 'Hyderabad', 1, 'https://www.mgit.ac.in'),
    ('SNIST', 'Sreenidhi Institute of Science and Technology', 'Ghatkesar', 'Medchal-Malkajgiri', 1, 'https://sreenidhi.edu.in'),
    ('MREC',  'Malla Reddy Engineering College', 'Maisammaguda, Secunderabad', 'Medchal-Malkajgiri', 1, 'https://www.mrec.ac.in'),
    ('CMR',   'CMR College of Engineering and Technology', 'Kandlakoya, Medchal', 'Medchal-Malkajgiri', 1, 'https://cmrcet.ac.in'),
    ('MLRIT', 'Marri Laxman Reddy Institute of Technology and Management', 'Dundigal, Hyderabad', 'Medchal-Malkajgiri', 1, 'https://www.mlrit.ac.in'),
    ('VJIT',  'Vidya Jyothi Institute of Technology', 'Aziz Nagar, Hyderabad', 'Ranga Reddy', 1, 'https://vjit.ac.in'),
    ('BVRIT', 'B V Raju Institute of Technology', 'Narsapur, Medak', 'Medak', 1, 'https://bvrit.ac.in')
) AS s(code, name, location, district, autonomous, website)
ON t.code = s.code
WHEN NOT MATCHED THEN
    INSERT (code, name, location, district, autonomous, website)
    VALUES (s.code, s.name, s.location, s.district, s.autonomous, s.website);
