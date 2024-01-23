// CUSTOM
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH (x)-[{uri:'http://purl.org/dc/elements/1.1/title'}]->(title) WHERE toString(title.value) =~ '(?i)web' RETURN title.stringrep $$) AS (title ag_catalog.agtype);

// RDF2PG
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH (x)-[{iri:'http://purl.org/dc/elements/1.1/title'}]->(title) WHERE toString(title.value) =~ '(?i)web' RETURN title.value $$) AS (title ag_catalog.agtype);
