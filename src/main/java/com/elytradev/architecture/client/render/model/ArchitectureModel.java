/*
 * MIT License
 *
 * Copyright (c) 2017 Benjamin K
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.elytradev.architecture.client.render.model;

import com.elytradev.architecture.client.render.target.RenderTargetBase;
import com.elytradev.architecture.client.render.texture.ITexture;
import com.elytradev.architecture.common.helpers.Trans3;
import com.elytradev.architecture.common.helpers.Vector3;
import com.google.gson.Gson;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class ArchitectureModel implements IArchitectureModel {

    private static final Gson GSON = new Gson();
    public double[] bounds;
    public Face[] faces;
    public double[][] boxes;

    public static ArchitectureModel fromResource(ResourceLocation location) {
        // Can't use resource manager because this needs to work on the server
        String path = String.format("/assets/%s/%s", location.getResourceDomain(), location.getResourcePath());
        InputStream in = ArchitectureModel.class.getResourceAsStream(path);
        ArchitectureModel model = GSON.fromJson(new InputStreamReader(in), ArchitectureModel.class);
        if (in == null)
            throw new RuntimeException("Model file not found: " + path);
        model.prepare();
        return model;
    }

    @Override
    public AxisAlignedBB getBounds() {
        return new AxisAlignedBB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
    }

    void prepare() {
        for (Face face : faces) {
            double[][] p = face.vertices;
            int[] t = face.triangles[0];
            face.normal = Vector3.unit(Vector3.sub(p[t[1]], p[t[0]]).cross(Vector3.sub(p[t[2]], p[t[0]])));
        }
    }

    @Override
    public void addBoxesToList(Trans3 t, List list) {
        if (boxes != null && boxes.length > 0) {
            for (int i = 0; i < boxes.length; i++)
                addBoxToList(boxes[i], t, list);
        } else
            addBoxToList(bounds, t, list);
    }

    protected void addBoxToList(double[] b, Trans3 t, List list) {
        t.addBox(b[0], b[1], b[2], b[3], b[4], b[5], list);
    }

    @Override
    public void render(Trans3 t, RenderTargetBase renderer, int baseColourMult, int secondaryColourMult, ITexture... textures) {
        Vector3 p = null, n = null;
        for (Face face : faces) {
            ITexture tex = textures[face.texture];
            if (tex != null) {
                renderer.setTexture(tex);
                for (int[] tri : face.triangles) {
                    renderer.beginTriangle();
                    for (int i = 0; i < 3; i++) {
                        int j = tri[i];
                        double[] c = face.vertices[j];
                        p = t.p(c[0], c[1], c[2]);
                        n = t.v(c[3], c[4], c[5]);
                        renderer.setNormal(n);
                        renderer.addVertex(p, c[6], c[7]);
                        float r, g, b;
                        boolean skipColour = false;
                        if (face.texture > 1) {
                            if (secondaryColourMult > -1) {
                                r = (float) (secondaryColourMult >> 16 & 255) / 255.0F;
                                g = (float) (secondaryColourMult >> 8 & 255) / 255.0F;
                                b = (float) (secondaryColourMult & 255) / 255.0F;
                                renderer.setColor(r, g, b, 1F);
                            }
                        } else {
                            if (baseColourMult > -1) {
                                r = (float) (baseColourMult >> 16 & 255) / 255.0F;
                                g = (float) (baseColourMult >> 8 & 255) / 255.0F;
                                b = (float) (baseColourMult & 255) / 255.0F;
                                renderer.setColor(r, g, b, 1F);
                            }
                        }
                    }
                    renderer.endFace();
                }
            }
        }
    }

    public static class Face {
        public int texture;
        double[][] vertices;
        int[][] triangles;
        //Vector3 centroid;
        Vector3 normal;
    }

}