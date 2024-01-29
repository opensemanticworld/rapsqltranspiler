# RAPSQLTranspiler

- [RAPSQLTranspiler](#rapsqltranspiler)
  - [Installation](#installation)
  - [Testing](#testing)
    - [W3C Test](#w3c-test)
    - [SP2B Test](#sp2b-test)
  - [RAPSQLBench Builds](#rapsqlbench-builds)
  - [License](#license)
  - [Authors](#authors)

## Installation

Perform install using maven (packaging and local repository install):

```bash
mvn clean install -DskipTests
```

Only packaging:

```bash
mvn clean package -DskipTests
```

## Testing

The automated tests depend on the correct `RDF2PG` integration maven build (specified in `pom.xml`, dependency groupId: `de.rapsql.rdf2pg`) to ensure that the proper model (either `yars` or `rdfid`) is used for the equality tests! The script `SparqlAlegebra.java` must also be adapted to the correct data model. The test Java scripts (`W3CTest.java` and `SP2BTest.java`) also have some configuration parameters that you should check and adjust before executing the tests to guarantee that all tests are executed correctly.

### W3C Test

```bash
mvn test -Dtest=W3CTest -Dstyle.color=never > logs/test-w3c-v0.3.0-yars-plain.log
mvn test -Dtest=W3CTest -Dstyle.color=never > logs/test-w3c-v0.3.1-yars-cpo1.log
mvn test -Dtest=W3CTest -Dstyle.color=never > logs/test-w3c-v0.3.2-yars-cpo2.log
mvn test -Dtest=W3CTest -Dstyle.color=never > logs/test-w3c-v0.3.3-yars-cpo3.log
mvn test -Dtest=W3CTest -Dstyle.color=never > logs/test-w3c-v0.4.0-rdfid-plain.log
mvn test -Dtest=W3CTest -Dstyle.color=never > logs/test-w3c-v0.4.1-rdfid-cpo1.log
mvn test -Dtest=W3CTest -Dstyle.color=never > logs/test-w3c-v0.4.2-rdfid-cpo2.log
mvn test -Dtest=W3CTest -Dstyle.color=never > logs/test-w3c-v0.4.3-rdfid-cpo3.log
```

### SP2B Test

```bash
mvn test -Dtest=SP2BTest -Dstyle.color=never > logs/test-sp2b-v0.3.0-yars-plain.log
mvn test -Dtest=SP2BTest -Dstyle.color=never > logs/test-sp2b-v0.3.1-yars-cpo1.log
mvn test -Dtest=SP2BTest -Dstyle.color=never > logs/test-sp2b-v0.3.2-yars-cpo2.log
mvn test -Dtest=SP2BTest -Dstyle.color=never > logs/test-sp2b-v0.3.3-yars-cpo3.log
mvn test -Dtest=SP2BTest -Dstyle.color=never > logs/test-sp2b-v0.4.0-rdfid-plain.log
mvn test -Dtest=SP2BTest -Dstyle.color=never > logs/test-sp2b-v0.4.1-rdfid-cpo1.log
mvn test -Dtest=SP2BTest -Dstyle.color=never > logs/test-sp2b-v0.4.2-rdfid-cpo2.log
mvn test -Dtest=SP2BTest -Dstyle.color=never > logs/test-sp2b-v0.4.3-rdfid-cpo3.log
```

## RAPSQLBench Builds

```bash
mvn clean install -Dstyle.color=never > logs/build-v0.3.0-yars-plain.log
mvn clean install -Dstyle.color=never > logs/build-v0.3.1-yars-cpo1.log
mvn clean install -Dstyle.color=never > logs/build-v0.3.2-yars-cpo2.log
mvn clean install -Dstyle.color=never > logs/build-v0.3.3-yars-cpo3.log
mvn clean install -Dstyle.color=never > logs/build-v0.4.0-rdfid-plain.log
mvn clean install -Dstyle.color=never > logs/build-v0.4.1-rdfid-cpo1.log
mvn clean install -Dstyle.color=never > logs/build-v0.4.2-rdfid-cpo2.log
mvn clean install -Dstyle.color=never > logs/build-v0.4.3-rdfid-cpo3.log
```

## License

Apache License 2.0

## Authors

Andreas Räder (<https://github.com/raederan>)
