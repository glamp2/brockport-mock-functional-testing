package funtionaltests;

import com.petstore.AnimalType;
import com.petstore.PetEntity;
import com.petstore.animals.CatEntity;
import com.petstore.animals.DogEntity;
import com.petstore.animals.attributes.*;
import com.petstoreservices.exceptions.PetDataStoreException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import static io.restassured.RestAssured.given;

public class AddPetEntityTests {

    private static Headers headers;

    @BeforeEach
    public void setup() throws PetDataStoreException {
        RestAssured.baseURI = "http://localhost:8080/";
        Header contentType = new Header("Content-Type", ContentType.JSON.toString());
        Header accept = new Header("Accept", ContentType.JSON.toString());
        headers = new Headers(contentType, accept);
    }

    @Test
    @DisplayName("Add New Cat - Success")
    public void addNewCatSuccessfully() {
        CatEntity newCat = new CatEntity(
                AnimalType.DOMESTIC,
                Skin.FUR,
                Gender.FEMALE,
                Breed.SPHYNX,
                new BigDecimal("350.00")
        );

        PetEntity response =
                given()
                        .headers(headers)
                        .body(newCat)
                        .when()
                        .post("/inventory/petType/CAT")
                        .then()
                        .log().all()
                        .assertThat().statusCode(200)
                        .assertThat().contentType(ContentType.JSON)
                        .extract()
                        .as(PetEntity.class);

    }

    @Test
    @DisplayName("Add New Dog - Success")
    public void addNewDogSuccessfully() {
        DogEntity newDog = new DogEntity(
                AnimalType.DOMESTIC,
                Skin.FUR,
                Gender.MALE,
                Breed.GERMAN_SHEPHERD,
                new BigDecimal("500.00")
        );

        PetEntity response =
                given()
                        .headers(headers)
                        .body(newDog)
                        .when()
                        .post("/inventory/petType/DOG")
                        .then()
                        .log().all()
                        .assertThat().statusCode(200)
                        .extract()
                        .as(PetEntity.class);
    }

    @Test
    @DisplayName("Add Pet with Invalid Type - Should Fail")
    public void addPetWithInvalidType() {
        CatEntity newPet = new CatEntity(
                AnimalType.DOMESTIC,
                Skin.FUR,
                Gender.FEMALE,
                Breed.SPHYNX,
                new BigDecimal("200.00")
        );

        given()
                .headers(headers)
                .body(newPet)
                .when()
                .post("/inventory/petType/INVALID_TYPE")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("Add Pet with Missing Required Fields - Should Fail")
    public void addPetWithMissingFields() {
        String incompleteJson = "{ \"gender\": \"FEMALE\", \"breed\": \"PERSIAN\" }";

        given()
                .headers(headers)
                .body(incompleteJson)
                .when()
                .post("/inventory/petType/CAT")
                .then()
                .statusCode(400);
    }
}