import os
import re
import argparse

warp_number = 4

parser = argparse.ArgumentParser()
parser.add_argument("--input_file", default='output.txt', help="log file from idea")
args = parser.parse_args()
if __name__ == '__main__':
    out_file = []
    print("test")
    print(args.input_file)
    for i in range(warp_number):
        out_file.append("warp"+str(i))
    with open(args.input_file, 'r') as f:
        print("open output_txt")
        for line in f.readlines():
            # open four new files and move matching output information to relevant file.
            if(line[0] != "\n"):
                for i in range(warp_number):
                    # print(line[1])
                    # print(str(i))
                    if re.match(str(i), line[5]):
                        with open(out_file[i], 'a') as wf:
                            wf.write(line)


        