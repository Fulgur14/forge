/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.cost;

import com.google.common.collect.Maps;
import forge.game.ability.AbilityKey;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import java.util.Map;

/**
 * The Class CostUntapType.
 */
public class CostUntapType extends CostPartWithList {

    private static final long serialVersionUID = 1L;
    public final boolean canUntapSource;

    public CostUntapType(final String amount, final String type, final String description, boolean hasUntapInPrice) {
        super(amount, type, description);
        canUntapSource = !hasUntapInPrice;
    }

    @Override
    public int paymentOrder() { return 18; }

    @Override
    public boolean isReusable() { return true; }

    @Override
    public boolean isRenewable() { return true; }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Untap ");

        final Integer i = convertAmount();
        final String desc = getDescriptiveType();

        sb.append(Cost.convertAmountTypeToWords(i, getAmount(), " tapped " + desc));

        if (getType().contains("OppCtrl")) {
            sb.append(" an opponent controls");
        }
        else if (getType().contains("YouCtrl")) {
            sb.append(" you control");
        }
        return sb.toString();
    }

    @Override
    public final void refund(final Card source) {
        for (final Card c : getCardList()) {
            c.setTapped(true);
        }
        resetLists();
    }

    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        final Player activator = ability.getActivatingPlayer();
        final Card source = ability.getHostCard();

        CardCollection typeList = CardLists.getValidCards(activator.getGame().getCardsIn(ZoneType.Battlefield), getType().split(";"), activator, source, ability);

        if (!canUntapSource) {
            typeList.remove(source);
        }
        typeList = CardLists.filter(typeList, CardPredicates.TAPPED, c -> c.getCounters(CounterEnumType.STUN) == 0 || c.canRemoveCounters(CounterType.get(CounterEnumType.STUN)));

        final int amount = this.getAbilityAmount(ability);
        return (typeList.size() != 0) && (typeList.size() >= amount);
    }

    @Override
    protected Card doPayment(Player payer, SpellAbility ability, Card targetCard, final boolean effect) {
        targetCard.untap();
        return targetCard;
    }

    @Override
    protected boolean canPayListAtOnce() {
        return true;
    }

    @Override
    protected CardCollectionView doListPayment(Player payer, SpellAbility ability, CardCollectionView targetCards, final boolean effect) {
        CardCollection untapped = new CardCollection();
        for (Card c : targetCards) {
            if (c.untap()) untapped.add(c);
        }
        if (!untapped.isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            final Map<Player, CardCollection> map = Maps.newHashMap();
            map.put(payer, untapped);
            runParams.put(AbilityKey.Map, map);
            payer.getGame().getTriggerHandler().runTrigger(TriggerType.UntapAll, runParams, false);
        }
        return targetCards;
    }
    @Override
    public String getHashForLKIList() {
        return "Untapped";
    }
    @Override
    public String getHashForCardList() {
    	return "UntappedCards";
    }

    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
