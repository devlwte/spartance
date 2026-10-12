package com.halo.decomp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import com.kyrn.spartan.R;

/** Original vector pictograms with the angular, cyan Spartan HUD treatment and Xbox vector buttons. */
final class TouchIcons {
    static Drawable drawA, drawB, drawX, drawY, drawCrouch, drawMenu, drawView, drawStick, drawDpad;
    static Drawable drawGrenade, drawVisible, drawHidden, drawFlashlight, drawSwap;

    static void init(Context ctx) {
        if (drawA == null && ctx != null) {
            try {
                drawA = ctx.getDrawable(R.drawable.ic_xbox_a);
                drawB = ctx.getDrawable(R.drawable.ic_xbox_b);
                drawX = ctx.getDrawable(R.drawable.ic_xbox_x);
                drawY = ctx.getDrawable(R.drawable.ic_xbox_y);
                drawCrouch = ctx.getDrawable(R.drawable.ic_crouch_bend);
                drawMenu = ctx.getDrawable(R.drawable.ic_xbox_menu);
                drawView = ctx.getDrawable(R.drawable.ic_xbox_view);
                drawStick = ctx.getDrawable(R.drawable.ic_xbox_stick);
                drawDpad = ctx.getDrawable(R.drawable.ic_xbox_dpad);
                drawGrenade = ctx.getDrawable(R.drawable.ic_hud_grenade);
                drawVisible = ctx.getDrawable(R.drawable.ic_hud_visible);
                drawHidden = ctx.getDrawable(R.drawable.ic_hud_hidden);
                drawFlashlight = ctx.getDrawable(R.drawable.ic_hud_flashlight);
                drawSwap = ctx.getDrawable(R.drawable.ic_hud_swap);
            } catch (Exception ignored) {}
        }
    }

    private static void lines(Canvas c, Paint p, float... points) {
        Path path = new Path();
        path.moveTo(points[0], points[1]);
        for (int i=2; i<points.length; i+=2) path.lineTo(points[i], points[i+1]);
        c.drawPath(path, p);
    }
    private static void arrow(Canvas c, Paint p) {
        lines(c,p,0,12,0,-12,-6,-6,0,-12,6,-6);
    }
    private static void spartan(Canvas c, Paint p, boolean crouched) {
        c.drawCircle(0,-11,3,p);
        if (crouched) {
            lines(c,p,0,-7,-4,1,6,4,2,12,10,12);
            lines(c,p,-4,1,-10,7,-5,12);
            lines(c,p,-1,-4,7,0,12,-3);
        } else {
            lines(c,p,0,-7,0,2,-7,12,-12,12);
            lines(c,p,0,2,7,9,12,6);
            lines(c,p,-11,-7,-5,-2,0,-6,6,-3,11,-9);
        }
    }
    static float opacity = 1.0f;

    static void draw(Canvas c, Paint p, int type, float x, float y, float radius, boolean active) {
        if (type <= 3) {
            Drawable d = (type == 0) ? drawA : (type == 1) ? drawB : (type == 2) ? drawX : drawY;
            if (d != null) {
                c.save();
                c.translate(x, y);
                int r = Math.round(radius);
                d.setBounds(-r, -r, r, r);
                int alpha = Math.max(15, (int)((active ? 255 : 215) * opacity));
                d.setAlpha(alpha);
                d.draw(c);
                c.restore();
                return;
            }
        }
        if (type == 5 && drawGrenade != null) {
            c.save();
            c.translate(x, y);
            int r = Math.round(radius);
            drawGrenade.setBounds(-r, -r, r, r);
            int alpha = Math.max(15, (int)((active ? 255 : 215) * opacity));
            drawGrenade.setAlpha(alpha);
            drawGrenade.draw(c);
            c.restore();
            return;
        }
        if (type == 6 && drawCrouch != null) {
            c.save();
            c.translate(x, y);
            int r = Math.round(radius);
            drawCrouch.setBounds(-r, -r, r, r);
            int alpha = Math.max(15, (int)((active ? 255 : 215) * opacity));
            drawCrouch.setAlpha(alpha);
            drawCrouch.draw(c);
            c.restore();
            return;
        }
        if (type == 8 && drawFlashlight != null) {
            c.save();
            c.translate(x, y);
            int r = Math.round(radius);
            drawFlashlight.setBounds(-r, -r, r, r);
            int alpha = Math.max(15, (int)((active ? 255 : 215) * opacity));
            drawFlashlight.setAlpha(alpha);
            drawFlashlight.draw(c);
            c.restore();
            return;
        }
        if (type == 9 && drawSwap != null) {
            c.save();
            c.translate(x, y);
            int r = Math.round(radius);
            drawSwap.setBounds(-r, -r, r, r);
            int alpha = Math.max(15, (int)((active ? 255 : 215) * opacity));
            drawSwap.setAlpha(alpha);
            drawSwap.draw(c);
            c.restore();
            return;
        }
        if (type == 10 && drawMenu != null) {
            c.save();
            c.translate(x, y);
            int r = Math.round(radius);
            drawMenu.setBounds(-r, -r, r, r);
            int alpha = Math.max(15, (int)((active ? 255 : 215) * opacity));
            drawMenu.setAlpha(alpha);
            drawMenu.draw(c);
            c.restore();
            return;
        }
        if (type == 11 && drawView != null) {
            c.save();
            c.translate(x, y);
            int r = Math.round(radius);
            drawView.setBounds(-r, -r, r, r);
            int alpha = Math.max(15, (int)((active ? 255 : 215) * opacity));
            drawView.setAlpha(alpha);
            drawView.draw(c);
            c.restore();
            return;
        }
        if (type == 19) {
            Drawable d = active ? drawVisible : drawHidden;
            if (d != null) {
                c.save();
                c.translate(x, y);
                int r = Math.round(radius);
                d.setBounds(-r, -r, r, r);
                int alpha = Math.max(15, (int)((active ? 255 : 215) * opacity));
                d.setAlpha(alpha);
                d.draw(c);
                c.restore();
                return;
            }
        }

        c.save(); c.translate(x,y); c.scale(radius/22f,radius/22f);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f);
        p.setStrokeJoin(Paint.Join.MITER); p.setStrokeCap(Paint.Cap.SQUARE);
        int base = active ? 0xffffffff : 0xffeaeaea;
        int a = Math.max(10, (int)(((base >>> 24) & 0xff) * opacity));
        p.setColor((a << 24) | (base & 0x00ffffff));
        switch(type) {
        case 0: // Xbox A
            p.setStrokeWidth(1.4f);
            c.drawCircle(0, 0, 14.5f, p);
            p.setStrokeWidth(2.6f);
            p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
            lines(c, p, -6.5f, 8.5f, -6.5f, 6.5f, -1.8f, -7.5f, 1.8f, -7.5f, 6.5f, 6.5f, 6.5f, 8.5f);
            lines(c, p, -4.2f, 2.2f, 4.2f, 2.2f);
            break;
        case 1: // Xbox B
            p.setStrokeWidth(1.4f);
            c.drawCircle(0, 0, 14.5f, p);
            p.setStrokeWidth(2.6f);
            p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
            Path pathB = new Path();
            pathB.moveTo(-5.5f, 8.5f); pathB.lineTo(-5.5f, -7.5f); pathB.lineTo(1.2f, -7.5f);
            pathB.cubicTo(5.4f, -7.5f, 5.4f, 0.2f, 1.2f, 0.2f); pathB.lineTo(-5.5f, 0.2f);
            pathB.moveTo(1.2f, 0.2f); pathB.cubicTo(6.0f, 0.2f, 6.0f, 8.5f, 1.2f, 8.5f); pathB.lineTo(-5.5f, 8.5f);
            c.drawPath(pathB, p);
            break;
        case 2: // Xbox X
            p.setStrokeWidth(1.4f);
            c.drawCircle(0, 0, 14.5f, p);
            p.setStrokeWidth(2.7f);
            p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
            lines(c, p, -6.5f, -7.5f, 6.5f, 7.5f);
            lines(c, p, 6.5f, -7.5f, -6.5f, 7.5f);
            break;
        case 3: // Xbox Y
            p.setStrokeWidth(1.4f);
            c.drawCircle(0, 0, 14.5f, p);
            p.setStrokeWidth(2.6f);
            p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
            lines(c, p, -6.5f, -7.5f, 0f, 0.5f, 6.5f, -7.5f);
            lines(c, p, 0f, 0.5f, 0f, 8.5f);
            break;
        case 4: case 17: // Fire: Tactical bullet cartridge
            p.setStrokeWidth(1.8f);
            p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
            Path bullet = new Path();
            bullet.moveTo(0f, -15.5f);
            bullet.cubicTo(-2.5f, -10f, -5f, -5f, -5f, -2.5f);
            bullet.lineTo(-5f, -0.5f); bullet.lineTo(-7.2f, 3.2f); bullet.lineTo(-7.2f, 12f);
            bullet.lineTo(-6f, 13f); bullet.lineTo(-6f, 14.5f); bullet.lineTo(-7.6f, 14.8f);
            bullet.lineTo(-7.6f, 16.5f); bullet.lineTo(7.6f, 16.5f); bullet.lineTo(7.6f, 14.8f);
            bullet.lineTo(6f, 14.5f); bullet.lineTo(6f, 13f); bullet.lineTo(7.2f, 12f);
            bullet.lineTo(7.2f, 3.2f); bullet.lineTo(5f, -0.5f); bullet.lineTo(5f, -2.5f);
            bullet.cubicTo(5f, -5f, 2.5f, -10f, 0f, -15.5f);
            c.drawPath(bullet, p);
            lines(c, p, -5f, -2.5f, 5f, -2.5f);
            lines(c, p, -7.2f, 3.2f, 7.2f, 3.2f);
            lines(c, p, -7.2f, 10.5f, 7.2f, 10.5f);
            lines(c, p, -10.5f, -6f, -13.5f, -1f);
            lines(c, p, 10.5f, -6f, 13.5f, -1f);
            break;
        case 5: // fragmentation grenade
            lines(c,p,-4,-8,-4,-13,3,-13,3,-8,9,-2,9,8,4,13,-4,13,-9,8,-9,-2,-4,-8);
            lines(c,p,3,-12,10,-9,11,-3); lines(c,p,-7,0,7,0); lines(c,p,-7,6,7,6); break;
        case 6: spartan(c,p,true); break;
        case 7: // scope reticle
            c.drawCircle(0,0,11,p); lines(c,p,-16,0,-5,0); lines(c,p,5,0,16,0);
            lines(c,p,0,-16,0,-5); lines(c,p,0,5,0,16); c.drawCircle(0,0,2,p); break;
        case 8: // flashlight
            lines(c,p,-13,-4,-3,-4,3,-9,3,9,-3,4,-13,4,-13,-4);
            lines(c,p,7,-6,14,-10); lines(c,p,8,0,16,0); lines(c,p,7,6,14,10); break;
        case 9: // swap frag / plasma grenades
            c.drawCircle(-6,0,5,p); lines(c,p,-8,-5,-8,-9,-4,-9,-4,-5);
            c.drawOval(3,-5,12,6,p); lines(c,p,6,-5,6,-8,9,-8,9,-5);
            lines(c,p,-12,12,10,12,6,8); lines(c,p,12,-13,-10,-13,-6,-9); break;
        case 10: // pause
            c.drawRect(-9,-12,-4,12,p); c.drawRect(4,-12,9,12,p); break;
        case 11: lines(c,p,12,0,-12,0,-4,-8,-12,0,-4,8); break;
        case 12: case 13: case 14: case 15:
            c.rotate(type==12 ? 0 : type==13 ? 180 : type==14 ? -90 : 90); arrow(c,p); break;
        case 16: // movement stick
            for (int i=0;i<4;i++) { c.save(); c.rotate(i*90); lines(c,p,-3,-9,0,-12,3,-9); c.restore(); }
            c.drawCircle(0,0,3,p); break;
        case 18: // camera modes
            lines(c,p,-12,-7,-5,-7,-2,-11,4,-11,7,-7,12,-7,12,7,-12,7,-12,-7);
            c.drawCircle(0,0,4,p); lines(c,p,-13,13,11,13,7,9); break;
        case 19: // show/hide overlay
            lines(c,p,-15,0,-7,-7,7,-7,15,0,7,7,-7,7,-15,0);
            c.drawCircle(0,0,4,p); if(active) lines(c,p,-13,13,13,-13); break;
        }
        p.setStyle(Paint.Style.FILL); c.restore();
    }
}
