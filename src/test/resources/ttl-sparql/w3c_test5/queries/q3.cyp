// CUSTOM
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH (x)-[{uri:'http://example.org/ns#price'}]->(price),(x)-[{uri:'http://purl.org/dc/elements/1.1/title'}]->(title) WHERE price.value < 30.5 RETURN title.stringrep, price.stringrep $$) AS (title ag_catalog.agtype, price ag_catalog.agtype);

// RDF2PG
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH (x)-[{iri:'http://example.org/ns#price'}]->(price),(x)-[{iri:'http://purl.org/dc/elements/1.1/title'}]->(title) WHERE toInteger(price.value) < 30.5 RETURN title.value, price.value $$) AS (title ag_catalog.agtype, price ag_catalog.agtype);

// TYPECHECK if not integer -> toFloat
SELECT * FROM ag_catalog.cypher('junit-test', $$ MATCH (x)-[{iri:'http://example.org/ns#price'}]->(price),(x)-[{iri:'http://purl.org/dc/elements/1.1/title'}]->(title) WHERE toFloat(price.value) < 30.5 RETURN title.value, price.value $$) AS (title ag_catalog.agtype, price ag_catalog.agtype);
