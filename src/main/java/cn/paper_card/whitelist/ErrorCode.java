package cn.paper_card.whitelist;

enum ErrorCode {
    Ok(),
    EmptyBody(),
    MissingArgument(),
    IllegalArgument(),
    ServiceUnavailable(),
    AlreadyWhitelisted(),
    NotWhitelist()
}
