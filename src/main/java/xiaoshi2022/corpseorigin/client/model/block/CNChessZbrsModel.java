package xiaoshi2022.corpseorigin.client.model.block;

import com.geckolib.model.DefaultedBlockGeoModel;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.entity.CNChessZbrsBlockEntity;

/**
 * 象棋尸兄模型。
 */
public class CNChessZbrsModel extends DefaultedBlockGeoModel<CNChessZbrsBlockEntity> {

    public CNChessZbrsModel() {
        super(CorpseOrigin.id("cn_chess_zbrs"));
    }

}