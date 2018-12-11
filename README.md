# Revolut Transfer Home Task
This is an implemenntation of tht Revolut home task.  The goal of this API is to transfer money between two accounts.  Routes are sturcture in a RESTful architecuter.

## Building
Running `gradle build` will build the application and place a single jar file at `build/libs/transfer-0.0.1-all.jar`.  This will also run tests and output an interactive view located at `./build/reports/tests/test/index.html`

## Running
To run the API, after building or by downloading the release from github, use the `java -jar transfer-0.0.1-all.jar` command.  If you're running from build use the following path: `build/libs/transfer-0.0.1-all.jar`

#### Docker
If you would like to run with docker run the following:
```shell
docker build -t revolut-transfer-stummej .
docker run -m512M --cpus 2 -it -p 8080:8080 --rm revolut-transfer-stummej
docker rmi revolut-transfer-stummej
```

### Features
* IBAN accounts can be created
* The balance of an account can be retrieved
* Money can be transfered between accounts (Note: This API assumes all money is in the same currency)
* A summary of all transfers from a source account can be retrieved
* Invalid IBAN detection (Only checksum)

## API Documentaion
#### Create account: 
Opens an account with an initial deposit

Request:
```
POST /accounts

{
    "countryCode": "GB",
    "initialDeposit": 50.00
}
```

Response:
```
{
    "accountNumber": "{UUID}"
}
```

#### Get account:
Gets the current status of the account

Request:
```
GET /accounts/{account number}
```

Response:
```
{
    "accountNumbre": "{UUID}",
    "balance": 0.00
}
```

#### Transfer:
Transfers money from the source account to a destination account

Request:
```
POST: /accounts/{source account}/transfer/{destination account}

{
    "amount": 0.00
}
```

Response:
```
{
    "transferId": "ec227d5f-4515-485b-a03f-3ccc98254ae5"
}
```

#### Get transfers:
Gets all transfers made by the sourrce account

Request:
```
GET: /accounts/{source account}/transfer
```

Response:
```
{
    "transfers": [
        {
            "id": "{UUID}",
            "source": "{source account number}",
            "destination": "{destination account number}",
            "amount": 0.00
        }
    ]
}
```

## Testing philosophy
With this project I stride to test using BTDD.  With this tests were writen in a way that the underlying implementation could change without having to change the tests.  Mocking/Test Doubles/Fakes, which are not used in this project, are only used at external boundaries where type contracts would seldom change.  Along with this if a test is purly functional then a unit test can fully cover the test, but if the test orchestrates functionality then it's covered by an integration or E2E test.  Some testing is duplicated between the unit and integration tests, but this proves a level of flexiblity that mocks can hinder.

## Dependencies
Kotlin - Language
Ktor - Web framework
Injekt - Dependency injection
Gson - JSON serializer/deserializer
Exposed - DSL/DAO
H2 - In memory database
KTest - Kotlin testing framework
Assertk - Fluent assertions (for collections)
ShadowJar - Single jar file
