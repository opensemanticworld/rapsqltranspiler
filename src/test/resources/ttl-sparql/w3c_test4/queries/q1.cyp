// CUSTOM
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH (x)-[{uri:'http://xmlns.com/foaf/0.1/name'}]->(name) RETURN x.stringrep, name.stringrep $$) AS (x ag_catalog.agtype, name ag_catalog.agtype);

//RDF2PG
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH (x)-[{iri:'http://xmlns.com/foaf/0.1/name'}]->(name) RETURN x.bnid, name.value $$) AS (x ag_catalog.agtype, name ag_catalog.agtype);
