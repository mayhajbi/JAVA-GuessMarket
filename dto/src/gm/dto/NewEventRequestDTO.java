package gm.dto;

/**
 * The details a user filled in to create a new event (bonus). The user becomes the market maker of
 * the event, and the event is created inactive - the same user then opens it like any other.
 * <p>
 * Only the fields of the chosen trading method are read: {@code liquidity} for an LMSR event, and
 * {@code baseValue}, {@code allowMint} and {@code initialInvestment} for an order book event. The
 * id of the event is decided by the engine, so that it can never collide with an existing one.
 *
 * @param userName          name of the user creating the event, who becomes its market maker
 * @param name              name of the event
 * @param description       what the event is about
 * @param commissionPercent the commission of the event, 0 - 90
 * @param commissionType    whether the commission is collected on every purchase or on close
 * @param firstOption       name of the first possible outcome
 * @param secondOption      name of the second possible outcome
 * @param type              the trading method of the event
 * @param liquidity         LMSR only: the liquidity value (b), a positive integer
 * @param baseValue         order book only: the base value (d), a positive integer
 * @param allowMint         order book only: whether minting new pairs of shares is allowed
 * @param initialInvestment order book only: what the market maker invests on opening, a multiple of
 *                          the base value
 */
public record NewEventRequestDTO(String userName,
                                 String name,
                                 String description,
                                 int commissionPercent,
                                 CommissionType commissionType,
                                 String firstOption,
                                 String secondOption,
                                 EventType type,
                                 int liquidity,
                                 int baseValue,
                                 boolean allowMint,
                                 int initialInvestment) {
}
