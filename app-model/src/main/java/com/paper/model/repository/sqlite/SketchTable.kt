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

package com.paper.model.repository.sqlite

import android.provider.BaseColumns

object SketchTable {

    // Table names.
    val TABLE_NAME: String = "paper"
    val TABLE_NAME_TEMP: String = "paper_temp"

    // Table columns.
    val COL_ID: String = BaseColumns._ID
    val COL_COUNT: String = BaseColumns._COUNT
    // Foreign key to sketch table.
    val COL_PAPER_ID: String = "paper_id"
//    val COL_WIDTH: String = "width"
//    val COL_HEIGHT: String = "height"
    val COL_THUMB_PATH: String = "thumb_path"
    val COL_THUMB_WIDTH: String = "thumb_width"
    val COL_THUMB_HEIGHT: String = "thumb_height"
    val COL_DATA_BLOB: String = "data"
}