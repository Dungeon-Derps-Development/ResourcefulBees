package com.teamresourceful.resourcefulbees.lib.enums;

import com.mojang.serialization.Codec;

public enum BaseModelType {
    DEFAULT,
    KITTEN;

    public static final Codec<BaseModelType> CODEC = Codec.STRING.xmap(BaseModelType::valueOf, BaseModelType::toString);
}
