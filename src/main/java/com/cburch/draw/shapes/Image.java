package com.cburch.draw.shapes;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Location;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static com.cburch.draw.Strings.S;

public class Image extends Rectangular {
    private String url;
    private java.awt.Image img;

    public Image(String url, int x, int y, int w, int h) {
        super(x, y, w, h);

        this.url = url;
        loadImage();

    }

    public String getUrl() {
        return url;
    }

    @Override
    public Element toSvgElement(Document doc) {
        return SvgCreator.createImage(doc, this);
    }

    @Override
    public String getDisplayName() {
        return S.get("shapeImage");
    }

    @Override
    protected boolean contains(int x, int y, int w, int h, Location q) {
        return isInRect(q.getX(), q.getY(), x, y, w, h);
    }

    @Override
    protected void draw(Graphics g, int x, int y, int w, int h) {
        if(img == null) {
            g.setColor(Color.BLACK);
            g.drawRect(x, y, w, h);
            return;
        }
        g.drawImage(this.img, x, y, x+w, y+h, 0, 0, img.getWidth(null), img.getHeight(null), null);
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return DrawAttr.ATTRS_IMAGE;
    }

    @Override
    public void updateValue(Attribute<?> attr, Object value) {
        super.updateValue(attr, value);
        if(attr == DrawAttr.URL) {
            url = (String) value;
            loadImage();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getValue(Attribute<V> attr) {
        if(attr == DrawAttr.URL) {
            return (V) url;
        }
        return super.getValue(attr);
    }

    private void loadImage() {
        try {
            img = ImageIO.read(new URL(url));
        } catch (IOException e) {
            System.err.println("Failed to open image at " + url);
        }
    }
}
