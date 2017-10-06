/*
 * Copyright (C) 2017 Jacksgong(jacksgong.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.dreamtobe.okdownload.core.connection;

import java.io.IOException;

/**
 * Created by Jacksgong on 24/09/2017.
 */

public class DownloadUrlConnection implements DownloadConnection {
    @Override
    public void addHeader(String name, String value) {
    }

    @Override
    public Connected execute() throws IOException {
        return null;
    }

    @Override
    public void release() {

    }

    public static class Factory implements DownloadConnection.Factory {

        @Override
        public DownloadConnection create(String url) {
            return new DownloadUrlConnection();
        }
    }
}