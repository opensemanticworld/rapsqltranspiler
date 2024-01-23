// CUSTOM
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH (v)-[p]->({uri:'', typeiri:'http://example.org/datatype#specialDatatype', lexform:'abc'}) RETURN v.stringrep $$) AS (v ag_catalog.agtype);

// RDF2PG
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH (v)-[p]->({type:'http://example.org/datatype#specialDatatype', value:'abc'}) RETURN v.iri $$) AS (v ag_catalog.agtype);
