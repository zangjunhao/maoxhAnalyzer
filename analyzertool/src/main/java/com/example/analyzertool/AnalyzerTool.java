package com.example.analyzertool;


import android.util.Log;

import com.android.tools.perflib.captures.MemoryMappedFileBuffer;
import com.squareup.haha.perflib.ArrayInstance;
import com.squareup.haha.perflib.ClassInstance;
import com.squareup.haha.perflib.ClassObj;
import com.squareup.haha.perflib.Heap;
import com.squareup.haha.perflib.Instance;
import com.squareup.haha.perflib.Snapshot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class AnalyzerTool {

    private static final String TAG = "maff";

    public AnalyzerTool(String dumpFilePath) throws IOException {
        // 打开hprof文件
        File file = new File(dumpFilePath);
        MemoryMappedFileBuffer buffer = new MemoryMappedFileBuffer(file);
        //解析获得快照
        com.squareup.haha.perflib.Snapshot snapshot = Snapshot.createSnapshot(buffer);
        snapshot.computeDominators();
        Collection<ClassObj> bitmapClasses = snapshot.findClasses("android.graphics.Bitmap");
        Collection<Heap> heaps = snapshot.getHeaps();
        Log.d(TAG, "AnalyzerTool: 1");
        for (Heap heap : heaps) {
            if (!heap.getName().equals("app") && !heap.getName().equals("default")) {
                continue;
            }
            HashMap<Integer, List<Instance>> bitMapHash = new HashMap<>();
            for (ClassObj classObj : bitmapClasses) {
                List<Instance> list = classObj.getHeapInstances(heap.getId());
                for (Instance instance : list) {
                    if (instance.getDistanceToGcRoot() == Integer.MAX_VALUE) {
                        continue;
                    }
                    Log.d(TAG, "AnalyzerTool: 2" + instance);
                    ArrayInstance mBuffer = XhHelper.fieldValue(((ClassInstance) instance).getValues(), "mBuffer");
                    int hashCode = Arrays.hashCode(mBuffer.getValues());//获取每个bitmap对象对应数组的hashcode
                    List<Instance> bitmapInstanceList;
                    if (bitMapHash.containsKey(hashCode)) {
                        bitmapInstanceList = bitMapHash.get(hashCode);
                    } else {
                        bitmapInstanceList = new ArrayList<>();
                    }
                    bitmapInstanceList.add(instance);
                    bitMapHash.put(hashCode, bitmapInstanceList);
                }

            }
            for (HashMap.Entry<Integer, List<Instance>> map : bitMapHash.entrySet()) {
                List<Instance> instancesList = map.getValue();
                if (instancesList.size() < 2) {
                    continue;
                }

                for (Instance tempInstance : instancesList) {
                    Log.d(TAG, "重复的图片为");
                    while (tempInstance.getNextInstanceToGcRoot() != null) {
                        Log.d(TAG, tempInstance.getNextInstanceToGcRoot() + "\n");
                        tempInstance = tempInstance.getNextInstanceToGcRoot();
                    }
                    Log.d(TAG, "结束");
                }
            }

        }

    }


}
