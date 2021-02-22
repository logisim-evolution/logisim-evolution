:
dist=$1
if [ -d $dist ] ; then
  cd $dist
  info=*.app/Contents/Info.plist
  awk '/Unknown/{sub(/Unknown/,"public.app-category.education")};{print};/NSHighResolutionCapable/{print "  <string>true</string>"; print "  <key>NSSupportsAutomaticGraphicsSwitching</key>"}' \
        $info > I.plist
  if [ -s I.plist ] ; then
    mv I.plist $info
  else
    echo "failed to insert automatic graphics switching" >&2
    exit 1
  fi
  codesign --remove-signature *.app
  if [ $? -ne 0 ] ; then
    echo "signature removal failed" >&2
    exit 1
  fi
else
  echo "distribution directory:" $dist " does not exist" >&2
  exit 1
fi
exit 0
