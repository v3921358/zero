/*
 * ArcStory Project
 * 理쒖＜??sch2307@naver.com
 * ?댁? junny_adm@naver.com
 * ?곗???raccoonfox69@gmail.com
 * 媛뺤젙洹?ku3135@nate.com
 * 源吏꾪솉 designer@inerve.kr
 */
package server.movement;

import java.awt.*;

public interface LifeMovement extends LifeMovementFragment {

    @Override
    Point getPosition();

    int getNewstate();

    int getDuration();

    int getType();

    short getFootHolds();
}
