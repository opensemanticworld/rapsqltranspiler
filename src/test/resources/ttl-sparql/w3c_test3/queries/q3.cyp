// CUSTOM
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH (v)-[p]->({uri:'', typeiri:'http://www.w3.org/2001/XMLSchema#integer', lexform:'42'}) RETURN v.stringrep $$) AS (v ag_catalog.agtype);

// RDF2PG
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH (v)-[p]->({type:'http://www.w3.org/2001/XMLSchema#integer', value:'42'}) RETURN v.iri $$) AS (v ag_catalog.agtype);
