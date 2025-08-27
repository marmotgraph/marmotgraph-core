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

package org.marmotgraph.commons.model;
import lombok.Getter;

import java.util.Date;
import java.util.UUID;

@Getter
public class ResultWithExecutionDetails<T> extends Result<T> {

    protected Long startTime;
    protected Long durationInMs;
    protected UUID transactionId;

    public static <T> ResultWithExecutionDetails<T> ok(){
        return ok(null, null);
    }

    public static <T> ResultWithExecutionDetails<T> ok(T data) {
        return ok(data, null);
    }

    public static <T> ResultWithExecutionDetails<T> ok(T data, String message) {
        ResultWithExecutionDetails<T> result = new ResultWithExecutionDetails<>();
        result.data = data;
        if (message != null) {
            result.message = message;
        }
        return result;
    }

    public static <T> ResultWithExecutionDetails<T> nok(int errorCode, String message){
        return nok(errorCode, message, null);
    }
    public static <T> ResultWithExecutionDetails<T> nok(int errorCode, String message, UUID uuid){
        ResultWithExecutionDetails<T> result = new ResultWithExecutionDetails<>();
        Error error = new Error();
        error.setCode(errorCode);
        error.setMessage(message);
        if(uuid!=null) {
            error.setInstanceId(uuid);
        }
        result.error = error;
        return result;
    }

    public ResultWithExecutionDetails<T> setExecutionDetails(Date startTime, Date endTime){
        this.startTime = startTime.getTime();
        this.durationInMs = endTime.getTime() - this.startTime;
        return this;
    }

}
