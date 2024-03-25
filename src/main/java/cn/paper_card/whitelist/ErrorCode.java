package cn.paper_card.whitelist;

enum ErrorCode {
    Ok(),
    Unknown(),
    EmptyBody(),
    MissingArgument(),
    IllegalArgument(),
    ServiceUnavailable(),
    AlreadyWhitelisted(),
    NotWhitelist(),
    NoWhitelistCode()
}
