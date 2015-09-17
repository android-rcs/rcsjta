/**
 * This API permits to discover capabilities supported by remote contacts.
 * <p>
 * This API allows for querying the capabilities of a user or users and checking
 * for changes in their capabilities:<br>
 * - Read the supported capabilities locally by the user on its device.<br>
 * - Retrieve all capabilities of a user.<br>
 * - Checking a specific capability of a user.<br>
 * - Registering for changes to a user/users's capabilities.<br>
 * - Unregistering for changes to a user/users's capabilities.<br>
 * - Define scheme for registering new service capabilities based on manifest defined
 *  feature tags. This API may be accessible by any application (third party, MNO, OEM).
 *  The RCS extensions are controlled internally by the RCS service.
 * <p>
 * For example, this API may be used:<br>
 * - To request capability update for a user when opening its contact card in the address book.<br>
 * - To synchronize capabilities of all the contacts from the RCS account management menu.<br>
 * - To receive supported capabilities when a CS call is established.<br>
 */

package com.gsma.services.rcs.capability;

