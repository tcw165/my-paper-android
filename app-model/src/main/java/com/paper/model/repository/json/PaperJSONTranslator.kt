// Copyright Mar 2018-present boyw165@gmail.com
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.

package com.paper.model.repository.json

import com.google.gson.*
import com.paper.model.IPaper
import com.paper.model.PaperAutoSaveImpl
import com.paper.model.Scrap
import com.paper.model.sketch.SketchStroke
import java.lang.reflect.Type

/**
 * Part of paper is stored as JSON.
 */
class PaperJSONTranslator : JsonSerializer<IPaper>,
                            JsonDeserializer<IPaper> {

    override fun serialize(src: IPaper,
                           typeOfSrc: Type,
                           context: JsonSerializationContext): JsonElement {
        val root = JsonObject()

        val sketchJson = JsonArray()
        src.getSketch().forEach { sketchJson.add(context.serialize(it, SketchStroke::class.java)) }
        root.add("sketch", sketchJson)

        val scrapJson = JsonArray()
        src.getScraps().forEach { scrapJson.add(context.serialize(it, Scrap::class.java)) }
        root.add("scraps", scrapJson)

        return root
    }

    override fun deserialize(json: JsonElement,
                             typeOfT: Type,
                             context: JsonDeserializationContext): IPaper {
        val root = json.asJsonObject
        val paperDetails = PaperAutoSaveImpl()

        if (root.has("sketch")) {
            root["sketch"].asJsonArray.forEach {
                paperDetails.pushStroke(context.deserialize(
                    it, SketchStroke::class.java))
            }
        }

        if (root.has("scraps")) {
            root["scraps"].asJsonArray.forEach {
                paperDetails.addScrap(context.deserialize(
                    it, Scrap::class.java))
            }
        }

        return paperDetails
    }
}
