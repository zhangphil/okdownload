/*
 * Copyright (c) 2017 LingoChamp Inc.
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

package com.liulishuo.okdownload.core.breakpoint;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.cause.EndCause;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RemitStoreOnSQLite extends BreakpointStoreOnSQLite
        implements RemitSyncToDBHelper.RemitAgent {

    @NonNull final RemitSyncToDBHelper remitHelper;
    final List<Integer> saveOnDBIdList = new ArrayList<>();

    RemitStoreOnSQLite(BreakpointSQLiteHelper helper, BreakpointStoreOnCache onCache,
                       @NonNull RemitSyncToDBHelper remitHelper) {
        super(helper, onCache);
        this.remitHelper = remitHelper;
        init();

    }

    RemitStoreOnSQLite(BreakpointSQLiteHelper helper, BreakpointStoreOnCache onCache) {
        super(helper, onCache);
        remitHelper = new RemitSyncToDBHelper(this);
        init();
    }

    public RemitStoreOnSQLite(Context context) {
        super(context);
        remitHelper = new RemitSyncToDBHelper(this);
        init();
    }

    void init() {
        final int size = onCache.storedInfos.size();
        for (int i = 0; i < size; i++) {
            saveOnDBIdList.add(onCache.storedInfos.keyAt(i));
        }
    }

    @NonNull @Override public BreakpointInfo createAndInsert(@NonNull DownloadTask task)
            throws IOException {
        if (remitHelper.isNotFreeToDatabase(task.getId())) return onCache.createAndInsert(task);

        final BreakpointInfo info = super.createAndInsert(task);
        saveOnDBIdList.add(info.getId());
        return info;
    }

    @Override public void onTaskStart(int id) {
        super.onTaskStart(id);
        remitHelper.onTaskStart(id);
    }

    @Override public void onSyncToFilesystemSuccess(@NonNull BreakpointInfo info, int blockIndex,
                                                    long increaseLength) {
        if (remitHelper.isNotFreeToDatabase(info.getId())) {
            onCache.onSyncToFilesystemSuccess(info, blockIndex, increaseLength);
            return;
        }

        super.onSyncToFilesystemSuccess(info, blockIndex, increaseLength);
    }

    @Override public boolean update(@NonNull BreakpointInfo info) throws IOException {
        if (remitHelper.isNotFreeToDatabase(info.getId())) return onCache.update(info);

        return super.update(info);
    }

    @Override
    public void onTaskEnd(int id, @NonNull EndCause cause, @Nullable Exception exception) {
        if (cause == EndCause.COMPLETED) {
            onCache.onTaskEnd(id, cause, exception);

            remitHelper.discardFlyingSyncOrEnsureSyncFinish(id);
            if (saveOnDBIdList.contains(id)) {
                // already on database
                helper.removeInfo(id);
                saveOnDBIdList.remove((Integer) id);
            }
        } else {
            remitHelper.ensureCacheToDB(id);
            onCache.onTaskEnd(id, cause, exception);
        }

        remitHelper.onTaskEnd(id);
    }

    @Override public void discard(int id) {
        onCache.discard(id);

        remitHelper.discardFlyingSyncOrEnsureSyncFinish(id);
        if (saveOnDBIdList.contains(id)) {
            // already on database
            helper.removeInfo(id);
            saveOnDBIdList.remove((Integer) id);
        }

        remitHelper.onTaskEnd(id);
    }

    @Override public void syncCacheToDB(int id) throws IOException {
        final BreakpointInfo info = onCache.get(id);
        if (info == null) return;

        helper.insert(info);
        saveOnDBIdList.add(id);
    }

    @Override public boolean isInfoNotOnDatabase(int id) {
        return !saveOnDBIdList.contains(id);
    }

    public static void setRemitToDBDelayMillis(int delayMillis) {
        final BreakpointStore store = OkDownload.with().breakpointStore();
        if (!(store instanceof RemitStoreOnSQLite)) {
            throw new IllegalStateException(
                    "The current store is " + store + " not RemitStoreOnSQLite!");
        }

        delayMillis = Math.max(0, delayMillis);
        ((RemitStoreOnSQLite) store).remitHelper.delayMillis = delayMillis;
    }
}