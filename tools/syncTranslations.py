#!/usr/bin/env python3

def getKeys(path):
    keys = []
    with open(path, 'r', encoding="utf8") as file:
        lines = file.readlines()
        for line in lines:
            if len(line) == 1:
                continue
            if line[0] == '#':
                keys.append(line.strip())
                continue
            words = line.split()
            keys.append(words[0])
    return keys


def getTrans(path):
    trans = {}
    with open(path, 'r', encoding="utf8") as file:
        lines = file.readlines()
        for line in lines:
            if line == '':
                continue
            if line[0] == '#':
                continue
            words = line.split('=')
            if len(words) <= 1:
                continue
            trans[words[0].strip()] = '='.join(words[1:]).strip()
    return trans


def writeFile(path, keys, trans):
    lines = []
    c = 0       # number of missing translations
    for key in keys:
        if key in trans:
            if trans[key] != '':
                lines.append(key + " = " + trans[key] + '\n')
                continue
        if key[0] == '#':
            lines.append(key + '\n')
        else:
            lines.append("# ==> " + key + " =\n")
            c += 1
    lines = ''.join(lines) + '\n\n'
    with open(path, 'w', encoding="utf8") as file:
        file.write(lines)
    return c
        
        
showMissing = False     # output number of missing translations
path = "../src/main/resources/resources/logisim/strings/"
files = ("analyze","circuit", "data", "draw", "file", "fpga", "gui", "hdl", "proj", "soc", "std", "tools", "util")
langs = ("de", "el", "es", "fr", "pt", "ru", "it", "nl", "ja", "pl")

for file in files:
    filePath = path + file +'/'+file
    if showMissing:
        print(file + ':')
    keys = getKeys(filePath + ".properties")
    for lang in langs:
        trans = getTrans(filePath+'_'+lang+".properties")
        missing = writeFile(filePath+'_'+lang+".properties", keys, trans)
        if showMissing and missing > 0:
            print("  " + lang + ": " + str(missing))
