:
appcontents=../images/*/*.app/Contents
if [ -d $appcontents ] ; then
  cd $appcontents
  if [ -f Info.plist ] ; then
    awk '/Unknown/{sub(/Unknown/,"public.app-category.education")};{print};/NSHighResolutionCapable/{print "  <string>true</string>"; print "  <key>NSSupportsAutomaticGraphicsSwitching</key>"}' \
        Info.plist > I.plist
    if [ -s I.plist ] ; then
      mv I.plist Info.plist
    fi
  fi
fi
exit 0
