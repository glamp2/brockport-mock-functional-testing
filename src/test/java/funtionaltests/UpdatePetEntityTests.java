package funtionaltests;

import com.petstore.AnimalType;
import com.petstore.PetEntity;
import com.petstore.PetStoreReader;
import com.petstore.animals.CatEntity;
import com.petstore.animals.attributes.Skin;
import com.petstoreservices.exceptions.PetDataStoreException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static io.restassured.RestAssured.given;


import com.petstore.animals.attributes.Breed;
import com.petstore.animals.attributes.Gender;
import static org.junit.jupiter.api.Assertions.*;

import com.petstore.animals.attributes.PetType;


public class UpdatePetEntityTests {

    private static Headers headers;
    private List<PetEntity> expectedResults;

    @BeforeEach
    public void retrieveDataStore() throws PetDataStoreException {
        RestAssured.baseURI = "http://localhost:8080/";
        PetStoreReader psReader = new PetStoreReader();
        expectedResults = psReader.readJsonFromFile();
        Header contentType = new Header("Content-Type", ContentType.JSON.toString());
        Header accept = new Header("Accept", ContentType.JSON.toString());
        headers = new Headers(contentType, accept);
    }


    @TestFactory
    @DisplayName("Update Pet Info - Success")
    public Stream<DynamicTest> updatePetPriceSuccess() throws PetDataStoreException {

        PetStoreReader psReader = new PetStoreReader();
        List<PetEntity> existingCats = psReader.readJsonFromFile().stream()
                .filter(p -> p.getPetType() == PetType.CAT)
                .collect(Collectors.toList());

        if(existingCats.isEmpty()) {
            fail("No cats found in test database");
        }

        PetEntity retrievedCat = existingCats.get(0);

        PetType petType = retrievedCat.getPetType();
        int existingCatId = retrievedCat.getPetId();

        System.out.println(retrievedCat);


        CatEntity updatedCat = new CatEntity(
                retrievedCat.getAnimalType(),  // Keep existing type
                retrievedCat.getSkinType(),        // Keep existing skin
                Gender.FEMALE,                    // Change gender (example)
                Breed.SPHYNX,                     // Change breed (example)
                new BigDecimal("350.00")          // New price
        );

        PetEntity response =
                given()
                        .headers(headers)
                        .queryParam("petType", petType)
                        .queryParam("petId", existingCatId)
                        .body(updatedCat)            // Attach updated cat as JSON
                        .when()
                        .put("/inventory/update")                    // HTTP PUT (full update)
                        .then()
                        .log().all()
                        .assertThat().statusCode(200) // Verify success
                        .assertThat().contentType("application/json")
                        .extract()
                        .jsonPath()
                        .getObject(".", PetEntity.class);


        List<DynamicTest> testResults = Arrays.asList(
                DynamicTest.dynamicTest("Change gender result test",
                        () -> assertEquals(Gender.FEMALE, response.getGender())),
                DynamicTest.dynamicTest("Change breed result test",
                        () -> assertEquals(Breed.SPHYNX, response.getBreed())),
                DynamicTest.dynamicTest("Change price result test",
                        () -> assertEquals(new BigDecimal("350.00"), response.getCost()))
        );
        return testResults.stream();
    }

    @Test
    @DisplayName("Update Non-Existent Cat - Adds New Cat")
    public void updateNonExistentCat() {
        PetType petType = PetType.CAT;
        int nonExistentId = 9999;

        CatEntity updatedCat = new CatEntity(
                AnimalType.DOMESTIC,
                Skin.FUR,
                Gender.MALE,
                Breed.SIAMESE,
                new BigDecimal("200.00")
        );

        given()
                .headers(headers)
                .queryParam("petType", petType)
                .queryParam("petId", nonExistentId)
                .body(updatedCat)
                .when()
                .put("/inventory/update")
                .then()
                .log().all()
                .statusCode(200);

    }
}