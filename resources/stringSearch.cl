__kernel void stringSearch(__global const char *text,
                           __global const char *pattern,
                           int patternSize,
                           __global const int *chars_per_item,
                           __global int *result) {

    int gid = get_global_id(0);

    int chars_in_current_item = chars_per_item[gid];

    int start = 0;

    if (gid > 0) {
        for (int i=0; i<gid; i++) {
            start += chars_per_item[i];
        }
    }

    int end = start + chars_in_current_item;

    int correctValues = 0;

for (int i=start; i<end; i++) {
   for(int y=0; y<patternSize; y++) {

        int pos = i+y;

        if (text[pos] == pattern[y]) {

            correctValues++;
        } else {

            correctValues = 0;
            i = pos;
            break;
        }
   }

   if (correctValues == patternSize) {
      break;
   }

   if (correctValues < patternSize && ((end - i) < (patternSize - correctValues))) {
      break;
   }
}

if (correctValues == patternSize) {
    result[gid] = 1;
} else {
    result[gid] = 0;
}

}