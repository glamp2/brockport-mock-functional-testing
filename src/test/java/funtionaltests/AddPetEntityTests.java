package funtionaltests;

import com.petstore.PetEntity;
import com.petstore.PetStoreReader;
import com.petstore.animals.attributes.PetType;
import com.petstoreservices.exceptions.PetDataStoreException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.parsing.Parser;

import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Add Pet Entity Tests functional and some error message tests.
 * The test class is using rest assured to help with functional testing
 */
public class AddPetEntityTests
{

    private static Headers headers;
    private List<PetEntity> expectedResults;
    @BeforeEach
    public void retrieveDataStore() throws PetDataStoreException
    {
        RestAssured.baseURI  = "http://localhost:8080/";
        PetStoreReader psReader = new PetStoreReader();
        expectedResults = psReader.readJsonFromFile();
        Header contentType = new Header("Content-Type", ContentType.JSON.toString());
        Header accept = new Header("Accept", ContentType.JSON.toString());
        headers = new Headers(contentType, accept);
    }

    @TestFactory
    @DisplayName("Add Pet Entity[Cat}")
    public Stream<DynamicTest> addCatTest() throws PetDataStoreException
    {
        //gather the expected results
        List<PetEntity> cats =
                expectedResults.stream()
                        .filter(p -> p.getPetType().equals(PetType.CAT))
                        .sorted(Comparator.comparingInt(PetEntity::getPetId))
                        .collect(Collectors.toList());
        if(cats.isEmpty()) //check for empty list, if empty fail test right away
        {
            fail("There is 0 remaining cats in the inventory. Test cannot be executed");
        }

        //Randomly generate an index from the size of the list
        Random random = new Random();
        int index = random.nextInt( cats.size());
        //generate the URI request
        String uri = "inventory/petType/CAT/petId/" + cats.get(index).getPetId();

        PetEntity addPet =
                given() //add the headers
                        .headers(headers)
                        .when()
                        .delete(uri)//execute the request
                        .then()
                        .log().all()
                        .assertThat().statusCode(200) //validate 200 Response, if not test will fail
                        .assertThat().contentType("application/json") //validate content type
                        .extract()//extract the response
                        .jsonPath()//as it is json set the path
                        .getObject(".", PetEntity.class); //As only expecting one item use getObject and return the entity


        PetStoreReader psReader = new PetStoreReader();
        List<PetEntity> actualResults = psReader.readJsonFromFile();
        List<DynamicTest> testResults= Arrays.asList(
                DynamicTest.dynamicTest("Size of results test[" + (expectedResults.size() - 1) + "]",
                        ()-> assertEquals((expectedResults.size() - 1), actualResults.size())),
                DynamicTest.dynamicTest("Pet Item Not in list[" + addPet.getPetId() + "]",
                        ()-> assertFalse(actualResults.contains(addPet)))
        );
        return testResults.stream();
    }
    @TestFactory
    @DisplayName("Add Pet Entity By Missing Pet Entity Tests")
    public Stream<DynamicTest> addInventoryMissingPetEntityTest()
    {
        RestAssured.registerParser("application/json", Parser.JSON);
        BadRequestResponseBody body =
                given()
                        .headers(headers)
                        .when()
                        .add("inventory/search/")
                        .then()
                        .log().all()
                        .assertThat().statusCode(404)
                        .extract()
                        .jsonPath().getObject(".", BadRequestResponseBody.class);

        return body.executeTests("Not Found", "No static resource inventory/search.",
                "/inventory/search/", 404).stream();
    }
    @TestFactory
    @DisplayName("Add Pet Entity By Invalid Pet Entity Tests")
    public Stream<DynamicTest> addInventoryInvalidPetEntityTest()
    {
        RestAssured.registerParser("application/json", Parser.JSON);
        BadRequestResponseBody body =
                given()
                        .headers(headers)
                        .when()
                        .
                        .add("inventory/search/FROGGER")
                        .then()
                        .log().all()
                        .assertThat().statusCode(400)
                        .extract()
                        .jsonPath()
                        .getObject(".", BadRequestResponseBody.class);

        return body.executeTests("Bad Request", "Failed to convert value of type 'java.lang.String' to " +
                        "required type 'com.petstore.animals.attributes.PetType'; Failed to convert from type " +
                        "[java.lang.String] to type [@org.springframework.web.bind.annotation.PathVariable " +
                        "com.petstore.animals.attributes.PetType] for value [FROGGER]",
                "/inventory/search/FROGGER", 400).stream();
    }
    @TestFactory
    @DisplayName("Add Pet Entity By Pet Id Not Exists Test")
    public Stream<DynamicTest> addInventoryPetEntityTest()
    {
        RestAssured.registerParser("application/json", Parser.JSON);
        List<PetEntity> cats =
                expectedResults.stream()
                        .filter(p -> p.getPetType().equals(PetType.CAT))
                        .sorted(Comparator.comparingInt(PetEntity::getPetId))
                        .collect(Collectors.toList());
        int index =  cats.size() + 100;
        BadRequestResponseBody body =
                given()
                        .headers(headers)
                        .when()
                        .add("inventory/search/" + index)
                        .then()
                        .log().all()
                        .assertThat().statusCode(400)
                        .extract()
                        .jsonPath().getObject(".", BadRequestResponseBody.class);

        return body.executeTests("Bad Request", "Failed to convert value of type 'java.lang.String' " +
                        "to required type 'com.petstore.animals.attributes.PetType'; Failed to convert from type " +
                        "[java.lang.String] to type [@org.springframework.web.bind.annotation.PathVariable com.petstore." +
                        "animals.attributes.PetType] for value [" + index + "]",
                "/inventory/search/" + index, 400).stream();
    }
}
