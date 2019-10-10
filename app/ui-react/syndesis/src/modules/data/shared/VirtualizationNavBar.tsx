import { RestDataService } from '@syndesis/models';
import { Container, TabBar, TabBarItem } from '@syndesis/ui';
import * as React from 'react';
import { useTranslation } from 'react-i18next';
import resolvers from '../resolvers';

/**
 * @param virtualization - the virtualization whose details are being shown by this page. If
 * exists, it must equal to the [virtualizationId]{@link IVirtualizationNavBarProps#virtualization}.
 */

export interface IVirtualizationNavBarProps {
  virtualization: RestDataService;
}

/**
 * A component that displays a nav bar with 4 items:
 *
 * 1. a link to the page that displays a list of Views
 * 2. a link to the page that displays the SQL Query editor
 *
 */
export const VirtualizationNavBar: React.FunctionComponent<
  IVirtualizationNavBarProps
> = props => {

  const { t } = useTranslation(['data', 'shared']);
  const virtualization = props.virtualization;

  return (
    <Container
      style={{
        background: '#fff',
      }}
    >
      <TabBar>
        <TabBarItem
          label={t('data:virtualization.views')}
          to={resolvers.virtualizations.views.root({
            virtualization,
          })}
        />
        <TabBarItem
          label={t('data:virtualization.sqlClient')}
          to={resolvers.virtualizations.sqlClient({
            virtualization,
          })}
        />
      </TabBar>
    </Container>
  );
}