//  Copyright Aug 2017-present boyw165@gmail.com
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package com.paper.model.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.paper.model.repository.json.SketchModelTranslator
import com.paper.model.repository.sqlite.SketchTable
import com.paper.model.sketch.SketchModel
import io.reactivex.Observable
import io.reactivex.Scheduler
import java.io.File

class SketchRepoSqliteImpl(authority: String,
                           resolver: ContentResolver,
                           cacheDirFile: File,
                           ioScheduler: Scheduler) : ISketchRepo {

    // Given...
    private val mAuthority: String = authority
    private val mResolver: ContentResolver = resolver
    private val mCacheDirFile = cacheDirFile
    private val mTempFile = File(cacheDirFile, authority + ".temp_sketch")
    private val mIoScheduler: Scheduler = ioScheduler

    // JSON translator.
    private val mGson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(SketchModel::class.java,
                                 SketchModelTranslator())
            .create()
    }

    override fun getSketchById(id: Long): Observable<SketchModel> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deletePaperById(id: Long): Observable<Boolean> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasTempSketch(): Observable<Boolean> {
        return Observable
            .fromCallable {
                mTempFile.exists()
            }
            .subscribeOn(mIoScheduler)
    }

    override fun getTempSketch(): Observable<SketchModel> {
        return Observable
            .fromCallable {
                mTempFile
                    .bufferedReader()
                    .use { reader ->
                        mGson.fromJson(reader, SketchModel::class.java)
                    }
            }
            .subscribeOn(mIoScheduler)
    }

    override fun newTempSketch(width: Int,
                               height: Int): Observable<SketchModel> {
        return Observable
            .fromCallable {
                val newSketch = SketchModel()
                val json = mGson.toJson(newSketch)

                mTempFile
                    .bufferedWriter()
                    .use { out ->
                        out.write(json)
                    }

                // Return.
                newSketch
            }
            .subscribeOn(mIoScheduler)
    }

    override fun newTempSketch(other: SketchModel): Observable<SketchModel> = saveTempSketch(other)

    override fun saveTempSketch(other: SketchModel): Observable<SketchModel> {
        return Observable
            .fromCallable {
                val json = mGson.toJson(other)

                mTempFile
                    .bufferedWriter()
                    .use { out ->
                        out.write(json)
                    }

                // Return.
                other
            }
            .subscribeOn(mIoScheduler)
    }

    override fun commitTempSketch(): Observable<SketchModel> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun convertSketchToValues(sketch: SketchModel): ContentValues {
        val values = ContentValues()

//        values.put(SketchTable.COL_WIDTH, sketch.width)
//        values.put(SketchTable.COL_HEIGHT, sketch.height)

        // FIXME:
        values.put(SketchTable.COL_THUMB_PATH, "")
        values.put(SketchTable.COL_THUMB_WIDTH, 0)
        values.put(SketchTable.COL_THUMB_HEIGHT, 0)
        // FIXME:
        values.put(SketchTable.COL_DATA_BLOB, "")

        return values
    }

    private fun convertCursorToSketch(cursor: Cursor): SketchModel {
        val paper = SketchModel()

//        val colOfId = cursor.getColumnIndexOrThrow(PaperTable.COL_ID)
//        paper.id = cursor.getLong(colOfId)
//
//        val colOfCreatedAt = cursor.getColumnIndexOrThrow(PaperTable.COL_CREATED_AT)
//        paper.createdAt = cursor.getLong(colOfCreatedAt)
//
//        val colOfModifiedAt = cursor.getColumnIndexOrThrow(PaperTable.COL_MODIFIED_AT)
//        paper.modifiedAt = cursor.getLong(colOfModifiedAt)
//
//        val colOfWidth = cursor.getColumnIndexOrThrow(PaperTable.COL_WIDTH)
//        paper.width = cursor.getInt(colOfWidth)
//
//        val colOfHeight = cursor.getColumnIndexOrThrow(PaperTable.COL_HEIGHT)
//        paper.height = cursor.getInt(colOfHeight)
//
//        val colOfCaption = cursor.getColumnIndexOrThrow(PaperTable.COL_CAPTION)
//        paper.caption = cursor.getString(colOfCaption)
//
//        val colOfThumb = cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_PATH)
//        paper.thumbnailPath = cursor.getString(colOfThumb)
//
//        val colOfThumbWidth = cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_WIDTH)
//        paper.thumbnailWidth = cursor.getInt(colOfThumbWidth)
//
//        val colOfThumbHeight = cursor.getColumnIndexOrThrow(PaperTable.COL_THUMB_HEIGHT)
//        paper.thumbnailHeight = cursor.getInt(colOfThumbHeight)

        return paper
    }
}