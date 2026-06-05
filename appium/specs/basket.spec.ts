import { driver } from '@wdio/globals';
import { byTestTag, dismissSystemDialogs, firstAddToBasket, tapNav, waitForAppReady } from '../helpers/app';
import { Selectors } from '../helpers/selectors';

describe('Basket — add via stepper and adjust quantity', () => {
  before(async () => {
    await dismissSystemDialogs();
    await waitForAppReady();
    await tapNav('navProducts');
    await byTestTag(Selectors.addToBasket, 30_000);
    const increase = await byTestTag(Selectors.stepperIncrease);
    await increase.click();
    await firstAddToBasket();
    await tapNav('navBasket');
  });

  it('shows basket line items after add', async () => {
    const line = await byTestTag(Selectors.basketLineItem, 15_000);
    await expect(line).toBeDisplayed();
  });

  it('increases quantity with stepper', async () => {
    const qtyBefore = await (await byTestTag(Selectors.stepperQuantity)).getText();
    await (await byTestTag(Selectors.stepperIncrease)).click();
    await driver.pause(400);
    const qtyAfter = await (await byTestTag(Selectors.stepperQuantity)).getText();
    expect(Number(qtyAfter)).toBeGreaterThan(Number(qtyBefore));
  });

  it('shows basket total', async () => {
    const total = await byTestTag(Selectors.basketTotal);
    await expect(total).toBeDisplayed();
    const text = await total.getText();
    expect(text).toMatch(/\$/);
  });
});
