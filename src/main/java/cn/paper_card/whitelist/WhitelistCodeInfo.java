package cn.paper_card.whitelist;

import java.util.UUID;

record WhitelistCodeInfo(
        int code,
        UUID uuid,
        String name,
        long createTime,
        long expireTime
) {
}
