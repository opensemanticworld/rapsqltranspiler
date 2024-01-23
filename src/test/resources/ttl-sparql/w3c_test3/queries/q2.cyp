// CUSTOM
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH (v)-[p]->({uri:'', typeiri:'http://www.w3.org/1999/02/22-rdf-syntax-ns#langString', lexform:'cat', langtag:'en'}) RETURN v.stringrep $$) AS (v ag_catalog.agtype);

// RDF2PG
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH (v)-[p]->({type:'http://www.w3.org/1999/02/22-rdf-syntax-ns#langString', value:'cat'}) RETURN v.iri $$) AS (v ag_catalog.agtype);

// LANGTAG=???