#!/usr/bin/env python3

#
# Checks *.properties files agains base english version.
# Reports missing translations. Can also update targed files
# with translation placeholders (with --write option).
#
# Usage example:
#
# To see current sync state:
#
#   cd <SRC>/tools
#   ./syncTranslations.py
#
# To update translation files
#
#   cd <SRC>/tools
#   ./syncTranslations.py --write
#
# When using with CWD being project root folder instead of tools/
#
#   ./syncTranslations.py --root src/main/resources/resources/logisim/strings/
#


import argparse
import os
import sys


def getKeys(path):
    keys = []
    with open(path, 'r', encoding = "utf8") as file:
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
    with open(path, 'r', encoding = "utf8") as file:
        lines = file.readlines()
        for line in lines:
            if line == '' or line[0] == '#':
                continue
            words = line.split('=')
            if len(words) <= 1:
                continue
            trans[words[0].strip()] = '='.join(words[1:]).strip()
    return trans


def writeFile(args, path, keys, trans):
    lines = []
    missingTranslations = 0
    for key in keys:
        if key in trans:
            if trans[key] != '':
                lines.append(key + " = " + trans[key] + '\n')
                continue
        if key[0] == '#':
            lines.append(key + '\n')
        else:
            lines.append("# ==> " + key + " =\n")
            missingTranslations += 1
    lines = ''.join(lines) + '\n\n'
    if (args.write):
        with open(path, 'w', encoding = "utf8") as file:
            file.write(lines)
    return missingTranslations


def main():
    defaultRootDir = "../src/main/resources/resources/logisim/strings/"
    files = sorted(["analyze", "circuit", "data", "draw", "file", "fpga", "gui", "hdl", "proj", "soc", "std", "tools", "util"])
    maxFileNameLen = max([len(i) for i in files]) + 2
    langs = sorted(["de", "el", "es", "fr", "pl", "pt", "ru", "it", "nl", "ja"])

    parser = argparse.ArgumentParser()
    group = parser.add_argument_group('Options')
    group.add_argument('--write', action = 'store_true', dest = 'write',
                       help = "Update translation files")
    group.add_argument('-q', '--quiet', action = 'store_true', dest = 'quiet')

    group = parser.add_argument_group('Paths')
    group.add_argument('--root', action = 'store', dest = 'rootDir', nargs = 1, metavar = 'DIR',
                       help = 'String resources root dir. Default: {}'.format(defaultRootDir))

    args = parser.parse_args()

    if args.rootDir is None:
        args.rootDir = defaultRootDir

    if not args.quiet:
        h1 = ' ' * maxFileNameLen + '|'
        h2 = '-' * maxFileNameLen + '+'
        for lang in langs:
            h1 += '{:^5} |'.format(lang.upper())
        h2 += '------+' * len(langs)
        print('{}\n{}'.format(h1, h2))

    for file in files:
        filePath = os.path.join(args.rootDir, file, file)
        if not args.quiet:
            fmt = '{:<' + str(maxFileNameLen) + '}|'
            print(fmt.format(file), end = '')
        keys = getKeys(filePath + ".properties")

        failed = False
        for lang in langs:
            trans = getTrans(filePath + '_' + lang + ".properties")
            missing = writeFile(args, filePath + '_' + lang + ".properties", keys, trans)
            failed = failed or missing
            if not args.quiet:
                if missing == 0:
                    missing = '-'
                print(' {:>4} |'.format(missing), end = '')
        if not args.quiet:
            print()

    if failed:
        sys.exit(100)
    else:
        sys.exit(0)


if __name__ == "__main__":
    main()
