// CUSTOM
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH (x)-[{uri:'http://xmlns.com/foaf/0.1/name'}]->(name),(x)-[{uri:'http://xmlns.com/foaf/0.1/mbox'}]->(mbox) RETURN name.stringrep, mbox.stringrep $$) AS (name ag_catalog.agtype, mbox ag_catalog.agtype);

// RDF2PG
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH (x)-[{iri:'http://xmlns.com/foaf/0.1/name'}]->(name),(x)-[{iri:'http://xmlns.com/foaf/0.1/mbox'}]->(mbox) RETURN name.value, mbox.iri $$) AS (name ag_catalog.agtype, mbox ag_catalog.agtype);

// EMAIL ADDRESS IS -- TYPE: URI -- IN JENA FUSEKI, e.g., <mailto:jlow@example.com>!!!