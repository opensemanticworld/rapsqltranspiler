# rapsqltranspiler

- [rapsqltranspiler](#rapsqltranspiler)
  - [Installation](#installation)
  - [Testing](#testing)
    - [W3C Test](#w3c-test)
    - [SP2B Test](#sp2b-test)
  - [Latest Build](#latest-build)
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

### W3C Test

```bash
mvn test -Dtest=W3CTest
```

### SP2B Test

```bash
mvn test -Dtest=SP2BTest
```

## Latest Build

```bash
mvn clean install -Dtest=SP2BTest -Dstyle.color=never > logs/buildlog-v0.2.1-rdfid.txt 
```

## License

Apache License 2.0

## Authors

Andreas Raeder
