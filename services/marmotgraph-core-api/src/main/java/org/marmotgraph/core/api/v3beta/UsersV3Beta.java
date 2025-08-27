/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2024 EBRAINS AISBL
 * Copyright 2024 - 2025 ETH Zurich
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  This open source software code was developed in part or in whole in the
 *  Human Brain Project, funded from the European Union's Horizon 2020
 *  Framework Programme for Research and Innovation under
 *  Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 *  (Human Brain Project SGA1, SGA2 and SGA3).
 */

package org.marmotgraph.core.api.v3beta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.marmotgraph.commons.IdUtils;
import org.marmotgraph.commons.Version;
import org.marmotgraph.commons.api.authentication.Authentication;
import org.marmotgraph.commons.api.primaryStore.Users;
import org.marmotgraph.commons.config.openApiGroups.Advanced;
import org.marmotgraph.commons.config.openApiGroups.Extra;
import org.marmotgraph.commons.config.openApiGroups.Simple;
import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.markers.ExposesConfigurationInformation;
import org.marmotgraph.commons.markers.ExposesUserInfo;
import org.marmotgraph.commons.markers.ExposesUserPicture;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.models.UserWithRoles;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * The user API allows to retrieve information how to access the authentication service and to retrieve information about the users.
 */
@RestController
@RequestMapping(Version.V3_BETA +"/users")
public class UsersV3Beta {

    private static final String ENDPOINT = "endpoint";
    private final Authentication.Client authentication;
    private final Users.Client primaryStoreUsers;

    public UsersV3Beta(Authentication.Client authentication, Users.Client primaryStoreUsers) {
        this.authentication = authentication;
        this.primaryStoreUsers = primaryStoreUsers;
    }

    @Operation(summary = "DEPRECATED: Get the endpoint of the authentication service - please use the harmonized endpoint at /setup/authorization")
    @GetMapping(value = "/authorization", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExposesConfigurationInformation
    @Extra
    @Deprecated(forRemoval = true)
    @SecurityRequirements
    public ResultWithExecutionDetails<JsonLdDoc> getAuthEndpoint() {
        JsonLdDoc ld = new JsonLdDoc();
        ld.addProperty(ENDPOINT, authentication.authEndpoint());
        return ResultWithExecutionDetails.ok(ld);
    }

    @Operation(summary = "DEPRECATED: Get the endpoint of the openid configuration - please use the harmonized endpoint at /setup/authorization")
    @GetMapping(value = "/authorization/config", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExposesConfigurationInformation
    @Advanced
    @Deprecated(forRemoval = true)
    @SecurityRequirements
    public ResultWithExecutionDetails<JsonLdDoc> getOpenIdConfigUrl() {
        JsonLdDoc ld = new JsonLdDoc();
        ld.addProperty(ENDPOINT, authentication.openIdConfigUrl());
        return ResultWithExecutionDetails.ok(ld);
    }

    @Operation(summary = "DEPRECATED: Get the endpoint to retrieve your token (e.g. via client id and client secret) - please use the harmonized endpoint at /setup/authorization")
    @GetMapping(value = "/authorization/tokenEndpoint", produces = MediaType.APPLICATION_JSON_VALUE)
    @ExposesConfigurationInformation
    @Extra
    @Deprecated(forRemoval = true)
    @SecurityRequirements
    public ResultWithExecutionDetails<JsonLdDoc> getTokenEndpoint() {
        JsonLdDoc ld = new JsonLdDoc();
        ld.addProperty(ENDPOINT, authentication.tokenEndpoint());
        return ResultWithExecutionDetails.ok(ld);
    }

    @Operation(summary = "Retrieve user information from the passed token (including detailed information such as e-mail address)")
    @GetMapping("/me")
    @ExposesUserInfo
    @Simple
    public ResponseEntity<ResultWithExecutionDetails<User>> myUserInfo() {
        User myUserInfo = authentication.getMyUserInfo();
        return myUserInfo!=null ? ResponseEntity.ok(ResultWithExecutionDetails.ok(myUserInfo)) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Retrieve the roles for the current user")
    @GetMapping("/me/roles")
    @ExposesUserInfo
    @Extra
    public ResponseEntity<ResultWithExecutionDetails<UserWithRoles>> myRoles() {
        final UserWithRoles roles = authentication.getRoles();
        return roles!=null ? ResponseEntity.ok(ResultWithExecutionDetails.ok(roles)) : ResponseEntity.notFound().build();
    }


    @Operation(summary = "Retrieve a list of users")
    @GetMapping
    @ExposesUserInfo
    @Deprecated(forRemoval = true)
    @Extra
    public ResponseEntity<PaginatedResult<NormalizedJsonLd>> getUserList(@ParameterObject PaginationParam paginationParam) {
        return ResponseEntity.badRequest().body(PaginatedResult.nokPaginated(HttpStatus.BAD_REQUEST.value(), "This method is no longer supported. Please resolve users via the IAM directly."));
    }

    @Operation(summary = "Retrieve a list of users from IAM with reduced information")
    @GetMapping("/fromIAM")
    @ExposesUserInfo
    @Extra
    @Deprecated(forRemoval = true)
    public ResponseEntity<ResultWithExecutionDetails<List<ReducedUserInformation>>>findUsers(@RequestParam("search") String search) {
        return ResponseEntity.badRequest().body(ResultWithExecutionDetails.nok(HttpStatus.BAD_REQUEST.value(), "This method is no longer supported. Please resolve users via the IAM directly."));
    }


    @Operation(summary = "Retrieve a list of users without sensitive information")
    @GetMapping("/limited")
    @ExposesUserInfo
    @Extra
    @Deprecated(forRemoval = true)
    public ResponseEntity<PaginatedResult<NormalizedJsonLd>> getUserListLimited(@ParameterObject PaginationParam paginationParam, @RequestParam(value = "id", required = false) String id) {
        Paginated<NormalizedJsonLd> users = this.primaryStoreUsers.getUsersWithLimitedInfo(paginationParam, id);
        users.getData().forEach(NormalizedJsonLd::removeAllInternalProperties);
        return ResponseEntity.ok(PaginatedResult.ok(users));
    }


    @Operation(summary = "Get the current terms of use", hidden = true)
    @GetMapping(value = "/termsOfUse")
    @Simple
    @Deprecated(forRemoval = true)
    public ResponseEntity<TermsOfUseResult> getTermsOfUse() {
        //Terms of use are no longer supported -> returning null
       return ResponseEntity.ok(null);
    }

    @Operation(summary = "Accept the terms of use in the given version", hidden = true)
    @PostMapping(value = "/termsOfUse/{version}/accept")
    @Simple
    @Deprecated(forRemoval = true)
    public void acceptTermsOfUse(@PathVariable("version") String version) {
        //Terms of use are no longer supported -> we don't do anything
    }

    @Operation(summary = "Get a pictures for a list of users (only found ones are returned)", hidden = true)
    @PostMapping(value = "/pictures")
    @ExposesUserPicture
    @Extra
    @Deprecated(forRemoval = true)
    public ResponseEntity<Map<UUID, String>> getUserPictures(@RequestBody List<UUID> userIds) {
        return ResponseEntity.ok(Collections.emptyMap());
    }


    @Operation(summary = "Get a picture for a specific user", hidden = true)
    @GetMapping(value = "/{id}/picture")
    @ExposesUserPicture
    @Extra
    @Deprecated(forRemoval = true)
    public ResponseEntity<String> getUserPicture(@PathVariable("id") UUID userId) {
        return ResponseEntity.notFound().build();
    }

    private UUID createUserPictureId(UUID userId){
        return IdUtils.createMetaRepresentationUUID(userId + "picture");
    }

    @Operation(summary = "Define a picture for a specific user", hidden = true)
    @PutMapping("/{id}/picture")
    @Extra
    @Deprecated(forRemoval = true)
    public ResponseEntity<ResultWithExecutionDetails<Void>> defineUserPicture(@PathVariable("id") UUID userId, @RequestBody String base64encodedImage) {
        return ResponseEntity.ok(ResultWithExecutionDetails.ok());
    }

}
