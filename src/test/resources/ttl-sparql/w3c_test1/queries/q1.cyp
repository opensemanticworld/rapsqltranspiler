// CUSTOM
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH ({uri:'http://example.org/book/book1'})-[{uri:'http://purl.org/dc/elements/1.1/title'}]->(title) RETURN title.stringrep $$) AS (title ag_catalog.agtype);

// RDF2PG
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH ({iri:'http://example.org/book/book1'})-[{iri:'http://purl.org/dc/elements/1.1/title'}]->(title) RETURN title.value $$) AS (title ag_catalog.agtype);
